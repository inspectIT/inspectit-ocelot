package rocks.inspectit.oce.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestUtils {

    /**
     * Returning the field with the given name of the given objects.
     *
     * @param object    the object containing the field
     * @param fieldname the name of the field
     * @param <T>       the type of the field
     * @return the field's object
     */
    public static <T> T resolve(Object object, String fieldname) {
        try {
            Field field = object.getClass().getDeclaredField(fieldname);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of methods existing in the given class and which are annotated with the specified annotation.
     *
     * @param clazz           the class for searching the methods
     * @param annotationClass the annotation of the methods
     * @return a {@link List} of methods matching the given attributes
     */
    public static List<Method> resolveMethodsByAnnotation(Class clazz, Class<? extends Annotation> annotationClass) {
        Method[] methods = clazz.getDeclaredMethods();

        return Arrays.stream(methods)
                .filter(method -> Arrays.stream(method.getAnnotations()).map(Annotation::annotationType).anyMatch(a -> a.equals(annotationClass)))
                .collect(Collectors.toList());
    }
}
