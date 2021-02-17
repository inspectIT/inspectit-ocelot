package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.DelegationMdcAccessor;

import java.lang.reflect.Method;

/**
 * Interface for all adapters for accessing the MDC of a given logging library.
 */
public interface MdcAdapter {

    Method getGetMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    Method getPutMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    Method getRemoveMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    DelegationMdcAccessor wrap(MdcAccessor mdcAccessor);

}
