package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

import java.lang.reflect.Method;

/**
 * Provides access to Log4j2s ThreadContext.
 */
@Slf4j
public class Log4J2MdcAdapter extends AbstractStaticMapMDCAdapter {

    /**
     * The name of the MDC class of Log4j2.
     */
    public static final String MDC_CLASS = "org.apache.logging.log4j.ThreadContext";

    private Log4J2MdcAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        super(put, get, remove);
    }

    public Log4J2MdcAdapter() {
    }

    /**
     * Creates an Adapater given a ThreadContext class.
     *
     * @param mdcClazz the org.apache.logging.log4j.ThreadContext class
     *
     * @return and adapter for setting values on the given thread context.
     */
    public static Log4J2MdcAdapter get(Class<?> mdcClazz) {
        try {
            WeakMethodReference put = WeakMethodReference.create(mdcClazz, "put", String.class, String.class);
            WeakMethodReference get = WeakMethodReference.create(mdcClazz, "get", String.class);
            WeakMethodReference remove = WeakMethodReference.create(mdcClazz, "remove", String.class);
            return new Log4J2MdcAdapter(put, get, remove);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("ThreadContext class did not contain expected methods", e);
        }
    }


    @Override
    public String getMDCClassName() {
        return "org.apache.logging.log4j.ThreadContext";
    }

    @Override
    public Method getGetMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("get", String.class);
    }

    @Override
    public Method getPutMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("put", String.class, String.class);
    }

    @Override
    public Method getRemoveMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("remove", String.class);
    }

    @Override
    public boolean isEnabledForConfig(TraceIdMDCInjectionSettings settings) {
        return settings.isLog4j2Enabled();
    }
}
