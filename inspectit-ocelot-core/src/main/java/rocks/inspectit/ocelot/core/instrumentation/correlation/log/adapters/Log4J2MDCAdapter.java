package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

/**
 * Provides access to Log4j2s ThreadContext.
 */
@Slf4j
public class Log4J2MDCAdapter extends AbstractStaticMapMDCAdapter {

    /**
     * The name of the MDC class of Log4j2.
     */
    public static final String THREAD_CONTEXT_CLASS = "org.apache.logging.log4j.ThreadContext";

    private Log4J2MDCAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        super(put, get, remove);
    }

    /**
     * Creates an Adapater given a ThreadContext class.
     *
     * @param mdcClazz the org.apache.logging.log4j.ThreadContext class
     *
     * @return and adapter for setting values on the given thread context.
     */
    public static Log4J2MDCAdapter get(Class<?> mdcClazz) {
        try {
            WeakMethodReference put = WeakMethodReference.create(mdcClazz, "put", String.class, String.class);
            WeakMethodReference get = WeakMethodReference.create(mdcClazz, "get", String.class);
            WeakMethodReference remove = WeakMethodReference.create(mdcClazz, "remove", String.class);
            return new Log4J2MDCAdapter(put, get, remove);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("ThreadContext class did not contain expected methods", e);
        }
    }

    @Override
    public boolean isEnabledForConfig(TraceIdMDCInjectionSettings settings) {
        return settings.isLog4j2Enabled();
    }
}
