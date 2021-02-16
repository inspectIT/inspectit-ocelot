package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.DelegationMdcAccessor;

import java.lang.reflect.Method;

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
    public DelegationMdcAccessor wrap(MdcAccessor mdcAccessor) {
        return new DelegationMdcAccessor(mdcAccessor) {
            @Override
            public boolean isEnabled(TraceIdMDCInjectionSettings settings) {
                return settings.isSlf4jEnabled();
            }
        };
    }
}
