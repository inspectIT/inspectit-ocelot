package rocks.inspectit.ocelot.core.instrumentation.actions.cache;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A specialized cache specifically for instances accessed via reflection.
 */
public class ReflectionCache {

    /** Cached fields */
    private final Map<FieldKey, Field> fieldCache = new ConcurrentHashMap<>();

    /** Cached methods */
    private final Map<MethodKey, Method> methodCache = new ConcurrentHashMap<>();

    /**
     * Access a specific field value via reflection. Can be called within actions via {@code _reflection}.
     *
     * @param clazz the class containing the field
     * @param instance the instance of the class
     * @param fieldName the name of the field
     *
     * @return the value of field
     */
    public Object getFieldValue(Class<?> clazz, Object instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = getField(clazz, fieldName);
        return field.get(instance);
    }

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
    public Object invokeMethod(Class<?> clazz, Object instance, String methodName, Object... args) throws ReflectiveOperationException {
        if (args == null) args = new Object[]{};

        Method method = getMethod(clazz, methodName, args);
        return method.invoke(instance, args);
    }

    private Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        FieldKey key = new FieldKey(clazz, fieldName);
        Field field = fieldCache.get(key);

        if (field == null) {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            fieldCache.putIfAbsent(key, field);
        }

        return field;
    }

    private Method getMethod(Class<?> clazz, String methodName, Object[] args)
            throws NoSuchMethodException {
        MethodKey key = new MethodKey(clazz, methodName, getArgTypes(args));
        Method method = methodCache.get(key);

        if (method == null) {
            method = findMatchingMethod(clazz, methodName, args);
            method.setAccessible(true);
            methodCache.putIfAbsent(key, method);
        }

        return method;
    }

    /**
     * Tries to find the specified method.
     *
     * @param clazz the class defining the method
     * @param methodName the name of the method
     * @param args the provided method arguments, if existing
     * @throws NoSuchMethodException if no method could be found
     *
     * @return the found method instance
     */
    private Method findMatchingMethod(Class<?> clazz, String methodName, Object[] args)
            throws NoSuchMethodException {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName)) continue;

            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != args.length) continue;

            // Check, if the parameter types match
            boolean match = true;
            for (int i = 0; i < paramTypes.length; i++) {
                if (args[i] != null && !paramTypes[i].isAssignableFrom(args[i].getClass())) {
                    match = false;
                    break;
                }
            }
            if (match) return method;
        }
        throw new NoSuchMethodException("Method " + methodName + " with matching parameters not found in " + clazz);
    }

    /**
     * Converts the array of objects to an array of the objects classes.
     *
     * @param args the method argument objects
     *
     * @return the array of argument classes
     */
    private Class<?>[] getArgTypes(Object[] args) {
        return Arrays.stream(args)
                .map(arg -> arg != null ? arg.getClass() : Object.class)
                .toArray(Class<?>[]::new);
    }

    /**
     * Contains the tuple of class and field name to identify one specific field.
     */
    private static final class FieldKey {

        private final Class<?> clazz;

        private final String fieldName;

        FieldKey(Class<?> clazz, String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof FieldKey)) return false;
            FieldKey other = (FieldKey) obj;
            return clazz.equals(other.clazz) && fieldName.equals(other.fieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, fieldName);
        }
    }

    /**
     * Contains the triple of class, method name and parameter types to identify one specific method.
     */
    private static final class MethodKey {

        private final Class<?> clazz;

        private final String methodName;

        private final Class<?>[] paramTypes;

        MethodKey(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.paramTypes = paramTypes != null ? paramTypes.clone() : new Class<?>[0];
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MethodKey)) return false;
            MethodKey other = (MethodKey) obj;
            return clazz.equals(other.clazz)
                    && methodName.equals(other.methodName)
                    && Arrays.equals(paramTypes, other.paramTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, methodName, Arrays.hashCode(paramTypes));
        }
    }
}
