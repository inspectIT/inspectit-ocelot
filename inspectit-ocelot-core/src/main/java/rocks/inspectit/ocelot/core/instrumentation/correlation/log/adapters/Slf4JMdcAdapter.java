package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.MdcAccessor;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Adapter for accessing the slf4j MDC class using reflection.
 * As Logback natively uses this MDC class it automatically includes logback support.
 */
@Slf4j
public class Slf4JMdcAdapter implements MdcAdapter {

    /**
     * The name of the SLF4J (and logback) MDC class
     */
    public static final String MDC_CLASS = "org.slf4j.MDC";

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
    public MdcAccessor createAccessor(WeakReference<Class<?>> mdcClass, BiConsumer<String, Object> putConsumer, Function<String, Object> getFunction, Consumer<String> removeConsumer) {
        return new MdcAccessor(mdcClass, putConsumer, getFunction, removeConsumer) {
            @Override
            public boolean isEnabled(TraceIdMDCInjectionSettings settings) {
                return settings.isSlf4jEnabled();
            }
        };
    }
}
