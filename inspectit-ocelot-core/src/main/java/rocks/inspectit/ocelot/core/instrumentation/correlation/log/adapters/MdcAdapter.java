package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.MDCAccess;

import java.lang.reflect.Method;

/**
 * Interface for all adapters for accessing the MDC of a given logging library.
 */
public interface MdcAdapter {

//    /**
//     * Writes a given entry to the MDC.
//     * If the value is null, the entry is removed from the MDC.
//     *
//     * @param key   the MDC key to use
//     * @param value the value to place in the MDC, null if the value for the key should be erased
//     *
//     * @return a {@link Undo} which reverts the change performed by this method call.
//     */
//    MDCAccess.Undo set(String key, String value);

    Method getGetMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    Method getPutMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    Method getRemoveMethod(Class<?> mdcClazz) throws NoSuchMethodException;

//    Class<? extends MdcAccessor> getAccessorClass();

    DelegationMdcAccessor wrap(MdcAccessor mdcAccessor);

    /**
     * Checks if this Adapter is enabled based on the given configuration.
     *
     * @param settings the currently active configuration
     *
     * @return true if enabled, false otherwise
     */
//    boolean isEnabledForConfig(TraceIdMDCInjectionSettings settings);
}
