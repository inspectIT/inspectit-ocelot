package rocks.inspectit.ocelot.core.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
     * @param field The final static {@link Field}
     * @param newValue
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static void setFinalStatic(Field field, Object newValue) throws NoSuchFieldException, IllegalAccessException {
        makeFieldAccessibleAndRemoveFinal(field);
        field.set(null, newValue);
    }

}
