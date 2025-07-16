package rocks.inspectit.ocelot.bootstrap.exposed;

/**
 * The reflection API, which is accessible from actions.
 * A specialized cache specifically for instances accessed via reflection.
 */
public interface InspectitReflection {

    /**
     * Access a specific field value via reflection. Can be called within actions via {@code _reflection}.
     *
     * @param clazz the class containing the field
     * @param instance the instance of the class
     * @param fieldName the name of the field
     *
     * @return the value of field
     */
    Object getFieldValue(Class<?> clazz, Object instance, String fieldName)
            throws NoSuchFieldException, IllegalAccessException;

    /**
     * Invokes a specific method via reflection. Can be called within actions via {@code _reflection}.
     *
     * @param clazz the class defining the method
     * @param instance the instance of the class
     * @param methodName the name of the method
     * @param args the provided method arguments, if existing
     *
     * @return the result of the invoked method
     */
    Object invokeMethod(Class<?> clazz, Object instance, String methodName, Object... args)
            throws ReflectiveOperationException;
}
