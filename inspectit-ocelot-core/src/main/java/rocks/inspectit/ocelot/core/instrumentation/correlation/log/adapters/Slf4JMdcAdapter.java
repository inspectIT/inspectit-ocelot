package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

import java.lang.reflect.Method;

/**
 * Adapter for accessing the slf4j MDC class using reflection.
 * As Logback natively uses this MDC class it automatically includes logback support.
 */
@Slf4j
public class Slf4JMdcAdapter extends AbstractStaticMapMDCAdapter {

    /**
     * The name of the SLF4J (and logback) MDC class
     */
    public static final String MDC_CLASS = "org.slf4j.MDC";

    private Slf4JMdcAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        super(put, get, remove);
    }

    public Slf4JMdcAdapter() {
    }

    /**
     * Creates an adapter for a given org.slf4j.MDC class.
     *
     * @param mdcClazz reference to the org.slf4j.MDC class
     * @return an adapter for the given class
     */
    public static Slf4JMdcAdapter get(Class<?> mdcClazz) {
        try {
            WeakMethodReference put = WeakMethodReference.create(mdcClazz, "put", String.class, String.class);
            WeakMethodReference get = WeakMethodReference.create(mdcClazz, "get", String.class);
            WeakMethodReference remove = WeakMethodReference.create(mdcClazz, "remove", String.class);
            return new Slf4JMdcAdapter(put, get, remove);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("MDC class did not contain expected methods", e);
        }
    }

    @Override
    public String getMDCClassName() {
        return "org.slf4j.MDC";
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
        return settings.isSlf4jEnabled();
    }
}
