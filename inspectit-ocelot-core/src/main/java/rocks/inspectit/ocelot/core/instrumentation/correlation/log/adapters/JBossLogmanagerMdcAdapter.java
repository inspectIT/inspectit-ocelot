package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;

import java.lang.reflect.Method;

/**
 * Provides access to JBoss Logmanagers ThreadContext.
 *
 * @author boris_unckel
 */
public class JBossLogmanagerMdcAdapter implements MdcAdapter {

    /**
     * The name of the MDC class of JBoss Logmanager.
     */
    public static final String MDC_CLASS = "org.jboss.logmanager.MDC";

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
                return false;
            }
        };
    }
}