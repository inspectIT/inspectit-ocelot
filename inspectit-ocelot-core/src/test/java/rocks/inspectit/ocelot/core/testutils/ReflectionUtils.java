package rocks.inspectit.ocelot.core.testutils;

import java.lang.reflect.Field;

public class ReflectionUtils {

    public static void writeField(Object target, String fieldName, Object value) {
        try {
            Class<?> targetClass = target.getClass();
            Field targetField = targetClass.getDeclaredField(fieldName);
            targetField.setAccessible(true);
            targetField.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeStaticField(Class<?> targetClass, String fieldName, Object value) {
        try {
            Field targetField = targetClass.getDeclaredField(fieldName);
            targetField.setAccessible(true);
            targetField.set(null, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
