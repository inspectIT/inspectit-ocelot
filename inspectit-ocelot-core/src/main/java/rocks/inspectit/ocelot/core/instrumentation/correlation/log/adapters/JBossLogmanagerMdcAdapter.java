package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.DelegationMdcAccessor;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

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
    public DelegationMdcAccessor wrap(BiConsumer<String, Object> putConsumer, Function<String, Object> getFunction, Consumer<String> removeConsumer) {
        return new DelegationMdcAccessor(putConsumer, getFunction, removeConsumer) {
            @Override
            public boolean isEnabled(TraceIdMDCInjectionSettings settings) {
                return settings.isJbossLogmanagerEnabled();
            }
        };
    }
}