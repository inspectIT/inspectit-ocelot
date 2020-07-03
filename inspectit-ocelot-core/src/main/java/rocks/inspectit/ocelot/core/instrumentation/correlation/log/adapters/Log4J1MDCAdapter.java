package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

/**
 * Provides access to Log4j1s ThreadContext.
 */
@Slf4j
public class Log4J1MDCAdapter extends AbstractStaticMapMDCAdapter {

    /**
     * The name of the MDC class of Log4j1.
     */
    public static final String MDC_CLASS = "org.apache.log4j.MDC";

    private Log4J1MDCAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        super(put, get, remove);
    }

    /**
     * Creates an Adapater given a org.apache.log4j.MDC class.
     *
     * @param mdcClazz the org.apache.log4j.MDC class
     *
     * @return and adapter for setting values on the given MDC
     */
    public static Log4J1MDCAdapter get(Class<?> mdcClazz) {
        try {
            WeakMethodReference put = WeakMethodReference.create(mdcClazz, "put", String.class, Object.class);
            WeakMethodReference get = WeakMethodReference.create(mdcClazz, "get", String.class);
            WeakMethodReference remove = WeakMethodReference.create(mdcClazz, "remove", String.class);
            return new Log4J1MDCAdapter(put, get, remove);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("MDC class did not contain expected methods", e);
        }
    }

    @Override
    public boolean isEnabledForConfig(TraceIdMDCInjectionSettings settings) {
        return settings.isLog4j1Enabled();
    }
}
