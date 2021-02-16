package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;

import java.lang.reflect.Method;

/**
 * Provides access to Log4j1s ThreadContext.
 */
@Slf4j
public class Log4J1MdcAdapter implements MdcAdapter {

    /**
     * The name of the MDC class of Log4j1.
     */
    public static final String MDC_CLASS = "org.apache.log4j.MDC";

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
    public DelegationMdcAccessor wrap(MdcAccessor mdcAccessor) {
        return new DelegationMdcAccessor(mdcAccessor) {
            @Override
            public boolean isEnabled(TraceIdMDCInjectionSettings settings) {
                return false;
            }
        };
    }

//    public static class Log4JDelegationMdcAccessor extends DelegationMdcAccessor {
//
//        public Log4JDelegationMdcAccessor(MdcAccessor mdcAccessor) {
//            super(mdcAccessor);
//        }
//
//        @Override
//        public boolean isEnabled(TraceIdMDCInjectionSettings settings) {
//            return false;
//        }
//    }

//    @Override
//    public boolean isEnabledForConfig(TraceIdMDCInjectionSettings settings) {
//        return settings.isLog4j1Enabled();
//    }
}
