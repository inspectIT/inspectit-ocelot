package rocks.inspectit.ocelot.config.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AutoboxingHelper {

    private static final Map<Class<?>, Class<?>> primitiveClassesToWrapperClassesMap;
    private static final Map<String, String> primitivesToWrappersMap;
    private static final Map<String, Class<?>> primitivesToPrimitiveClassesMap;
    private static final Map<String, String> wrappersToPrimitivesMap;

    private static final Map<String, String> wrappersSimpleNametoFQNMap;

    static {
        primitiveClassesToWrapperClassesMap = new HashMap<>();
        primitiveClassesToWrapperClassesMap.put(byte.class, Byte.class);
        primitiveClassesToWrapperClassesMap.put(short.class, Short.class);
        primitiveClassesToWrapperClassesMap.put(int.class, Integer.class);
        primitiveClassesToWrapperClassesMap.put(long.class, Long.class);
        primitiveClassesToWrapperClassesMap.put(float.class, Float.class);
        primitiveClassesToWrapperClassesMap.put(double.class, Double.class);
        primitiveClassesToWrapperClassesMap.put(char.class, Character.class);

        primitivesToPrimitiveClassesMap = new HashMap<>();
        primitiveClassesToWrapperClassesMap.forEach((p, w) -> primitivesToPrimitiveClassesMap.put(p.getName(), p));

        primitivesToWrappersMap = new HashMap<>();
        primitiveClassesToWrapperClassesMap.forEach((p, w) -> primitivesToWrappersMap.put(p.getName(), w.getName()));

        wrappersToPrimitivesMap = new HashMap<>();
        primitivesToWrappersMap.forEach((p, w) -> wrappersToPrimitivesMap.put(w, p));

        wrappersSimpleNametoFQNMap = new HashMap<>();
        primitiveClassesToWrapperClassesMap.values().forEach(w -> wrappersSimpleNametoFQNMap.put(w.getSimpleName(), w.getName()));

    }

    public static Set<String> getPrimitives() {
        return primitivesToWrappersMap.keySet();
    }

    public static Set<String> getWrapperFQNs() {
        return wrappersToPrimitivesMap.keySet();
    }

    public static Set<String> getWrapperSimpleNames() {
        return wrappersToPrimitivesMap.keySet();
    }

    /**
     * Returns the corresponding Wrapper class for the given type
     *
     * @param typeName the primitive or the corresponding wrapper class name, either simple or FQN
     * @return the corresponding wrapper class
     */
    public static Class<?> getWrapperClass(String typeName) {
        String primitiveType = null;
        if (isPrimitiveType(typeName)) {
            primitiveType = typeName;
        } else if (wrappersSimpleNametoFQNMap.containsKey(typeName)) {
            primitiveType = wrappersToPrimitivesMap.get(wrappersSimpleNametoFQNMap.get(typeName));
        } else {
            primitiveType = wrappersSimpleNametoFQNMap.get(typeName);
        }
        Class<?> primClass = primitivesToPrimitiveClassesMap.get(primitiveType);
        return primitiveClassesToWrapperClassesMap.get(primClass);
    }

    /**
     * @param typename the name of the type
     * @return true, if the given type is a primitive
     */
    public static boolean isPrimitiveType(String typename) {
        return primitivesToWrappersMap.containsKey(typename);
    }

    /**
     * @param typename the name of the class (either simple or FQN)
     * @return true, if the given type is a primitive wrapper
     */
    public static boolean isWrapperType(String typename) {
        return wrappersToPrimitivesMap.containsKey(typename) || wrappersSimpleNametoFQNMap.containsKey(typename);
    }

    /**
     * @param wrapper the typename of the wrapper class, either simple or FQN
     * @return the primitive typename this wrapper wraps
     */
    public static String getPrimitiveForWrapper(String wrapper) {
        if (!wrappersToPrimitivesMap.containsKey(wrapper)) {
            wrapper = wrappersSimpleNametoFQNMap.get(wrapper);
        }
        return wrappersToPrimitivesMap.get(wrapper);
    }

    /**
     * @param primitive the name of the primitive type
     * @return the FQN of the wrapper type
     */
    public static String getWrapperForPrimitive(String primitive) {
        return primitivesToWrappersMap.get(primitive);
    }

    /**
     * Reutrn the mthod of a wrapper class which performs the unboxing, e.g. intValue for Integer, longValue for Long etc.
     *
     * @param wrapper the name of the wrapper, either FQN or simple
     * @return the name of the unboxing method
     */
    public static String getWrapperUnboxingMethodName(String wrapper) {
        String primitive = getPrimitiveForWrapper(wrapper);
        if (primitive != null) {
            return primitive + "Value";
        } else {
            return null;
        }
    }

    public static Class<?> getPrimitiveClass(String typename) {
        return primitivesToPrimitiveClassesMap.get(typename);
    }
}
