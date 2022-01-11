package rocks.inspectit.ocelot.core.utils;

import lombok.extern.slf4j.Slf4j;

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
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
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
        field = makeFieldAccessibleAndRemoveFinal(field);
        field.set(null, newValue);
    }

    /**
     * Gets the final static field for the given {@link Class}, makes it accessible
     *
     * @param clazz       The {@link Class} that contains the final static field
     * @param fieldName   The name of the field
     * @param removeFinal Whether to remove the {@link Modifier#FINAL} modifier from the field
     *
     * @return
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */

    public static Field getFinalStaticFieldAndMakeAccessible(Class clazz, String fieldName, boolean removeFinal) throws NoSuchFieldException, IllegalAccessException {
        Field field = null;

        field = clazz.getDeclaredField(fieldName);

        // make field accessible
        if (removeFinal) {
            ReflectionUtils.makeFieldAccessibleAndRemoveFinal(field);
        } else {
            field.setAccessible(true);
        }

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
