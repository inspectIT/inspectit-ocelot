package rocks.inspectit.oce.core;

import java.lang.reflect.Field;

public class TestUtils {

    public static <T> T resolve(Object object, String fieldname) {
        try {
            Field field = object.getClass().getDeclaredField(fieldname);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
