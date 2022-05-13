package rocks.inspectit.ocelot.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Slf4j
public class ReflectionUtils {

    /**
     * Makes the {@link Field} accessible and removes the {@link Modifier#FINAL}
     *
     * @param field
     *
     * @return The {@link Field} with updated modifiers
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static Field makeFieldAccessibleAndRemoveFinal(Field field) throws NoSuchFieldException, IllegalAccessException {
        field.setAccessible(true);
        FieldUtils.removeFinalModifier(field);
        return field;
    }

    /**
     * Sets the value of the final static {@link Field} via reflection and access modification
     *
     * @param field    The final static {@link Field}
     * @param newValue
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static void setFinalStatic(Field field, Object newValue) throws NoSuchFieldException, IllegalAccessException {
        // see https://stackoverflow.com/questions/61141836/change-static-final-field-in-java-12
        if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_12)) {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            Object staticFieldBase = unsafe.staticFieldBase(field);
            long staticFieldOffset = unsafe.staticFieldOffset(field);
            unsafe.putObject(staticFieldBase, staticFieldOffset, newValue);
        } else {
            field = makeFieldAccessibleAndRemoveFinal(field);
            field.set(null, newValue);
        }
    }

    /**
     * Gets the field for the given {@link Class}, makes it accessible
     *
     * @param clazz     The {@link Class} that contains the final static field
     * @param fieldName The name of the field
     *
     * @return the specified field of the given class
     */

    public static Field getAccessibleField(Class clazz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    /**
     * Gets the {@link Field} for the given private field of the given {@link Class}
     *
     * @param clazz     The {@link Class}
     * @param fieldName The name of the private field
     *
     * @return
     *
     * @throws NoSuchFieldException
     */
    public static Field getPrivateField(Class clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

}
