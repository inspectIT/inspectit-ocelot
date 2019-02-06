package rocks.inspectit.oce.core.utils;

import java.util.HashMap;
import java.util.Map;

public class AutoboxingHelper {

    private static final Map<Class<?>, Class<?>> primitiveClassesToWrapperClassesMap;
    private static final Map<String, String> primitivesToWrappersMap;
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

        primitivesToWrappersMap = new HashMap<>();
        primitiveClassesToWrapperClassesMap.forEach((p, w) -> primitivesToWrappersMap.put(p.getName(), w.getName()));

        wrappersToPrimitivesMap = new HashMap<>();
        primitivesToWrappersMap.forEach((p, w) -> wrappersToPrimitivesMap.put(w, p));

        wrappersSimpleNametoFQNMap = new HashMap<>();
        primitiveClassesToWrapperClassesMap.values().forEach(w -> wrappersSimpleNametoFQNMap.put(w.getSimpleName(), w.getName()));
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
}
