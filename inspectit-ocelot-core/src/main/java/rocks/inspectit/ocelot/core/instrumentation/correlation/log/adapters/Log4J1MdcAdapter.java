package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

import java.lang.reflect.Method;

/**
 * Provides access to Log4j1s ThreadContext.
 */
@Slf4j
public class Log4J1MdcAdapter extends AbstractStaticMapMDCAdapter {

    /**
     * The name of the MDC class of Log4j1.
     */
    public static final String MDC_CLASS = "org.apache.log4j.MDC";

    private Log4J1MdcAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        super(put, get, remove);
    }

    public Log4J1MdcAdapter() {
    }

    /**
     * Creates an Adapater given a org.apache.log4j.MDC class.
     *
     * @param mdcClazz the org.apache.log4j.MDC class
     *
     * @return and adapter for setting values on the given MDC
     */
    public static Log4J1MdcAdapter get(Class<?> mdcClazz) {
        try {
            WeakMethodReference put = WeakMethodReference.create(mdcClazz, "put", String.class, Object.class);
            WeakMethodReference get = WeakMethodReference.create(mdcClazz, "get", String.class);
            WeakMethodReference remove = WeakMethodReference.create(mdcClazz, "remove", String.class);
            return new Log4J1MdcAdapter(put, get, remove);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("MDC class did not contain expected methods", e);
        }
    }

    @Override
    public String getMDCClassName() {
        return "org.apache.log4j.MDC";
    }

    @Override
    public Method getGetMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("get", String.class);
    }

    @Override
    public Method getPutMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("put", String.class, Object.class);
    }

    @Override
    public Method getRemoveMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("remove", String.class);
    }

    @Override
    public boolean isEnabledForConfig(TraceIdMDCInjectionSettings settings) {
        return settings.isLog4j1Enabled();
    }
}
