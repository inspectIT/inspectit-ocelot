package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import rocks.inspectit.ocelot.core.instrumentation.correlation.log.DelegationMdcAccessor;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interface for all adapters for accessing the MDC of a given logging library.
 */
public interface MdcAdapter {

    Method getGetMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    Method getPutMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    Method getRemoveMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    DelegationMdcAccessor wrap(BiConsumer<String, Object> putConsumer, Function<String, Object> getFunction, Consumer<String> removeConsumer);

//    void wrap(BiConsumer<String, Object> putConsumer, Function<String, Object> getFunction, Consumer<String> removeConsumer);
}
