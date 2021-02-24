package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import rocks.inspectit.ocelot.core.instrumentation.correlation.log.MdcAccessor;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interface for all adapters for accessing the MDC of a given logging library.
 */
public interface MdcAdapter {

    /**
     * Returns the method which is used for getting a specific element of the underlying MDC. The returned method
     * has to accept a single argument of type {@link String} and returning an object.
     *
     * @param mdcClazz the loaded MDC class
     * @return the method for accessing element in the MDC
     * @throws NoSuchMethodException in case the specified method cannot be found
     */
    Method getGetMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    /**
     * Returns the method which is used for putting a specific element into the underlying MDC. The returned method
     * has to accept two arguments of type {@link String} - representing the element's key - and of type {@link Object}
     * - representing the value. The method must not have a return value.
     *
     * @param mdcClazz the loaded MDC class
     * @return the method for putting element into the MDC
     * @throws NoSuchMethodException in case the specified method cannot be found
     */
    Method getPutMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    /**
     * Returns the method which is used for removing a specific element from the underlying MDC. The returned method
     * has to accept a single argument of type {@link String} - representing the element's key - and has no return value.
     *
     * @param mdcClazz the loaded MDC class
     * @return the method for removing elements from the MDC
     * @throws NoSuchMethodException in case the specified method cannot be found
     */
    Method getRemoveMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    /**
     * Creates a new instance of a {@link MdcAccessor} which is related to the MDC of this adapter. The accessor
     * contains the consumers and functions representing the GET, PUT and REMOVE interaction with the MDC.
     *
     * @param targetMdcClass the MDC class - weakly referenced for preventing that classes cannot be gc'ed.
     * @param putConsumer    consumer for putting elements into the MDC
     * @param getFunction    function for getting elements of the MDC
     * @param removeConsumer consumer for removing element from the MDC
     * @return the {@link MdcAccessor} instance for interacting with the MDC
     */
    MdcAccessor createAccessor(WeakReference<Class<?>> targetMdcClass, BiConsumer<String, Object> putConsumer, Function<String, Object> getFunction, Consumer<String> removeConsumer);
}
