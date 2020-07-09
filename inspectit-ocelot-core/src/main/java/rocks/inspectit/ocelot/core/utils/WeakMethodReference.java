package rocks.inspectit.ocelot.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * Weakly references a method via reflection.
 * This means that this reference can be held without causing a memory leak.
 * Note: THE METHOD PARAMETERS ARE NOT WEAKLY REFERENCED!
 * This means this class should be used only for methods using bootstrap types as parameters.
 */
@Slf4j
public class WeakMethodReference {

    private WeakReference<Class<?>> declaringClass;

    private String name;

    private Class<?>[] parameterTypes;

    private WeakReference<Method> methodReference;

    private WeakMethodReference(Class<?> declaringClass, String name, Class<?>[] parameters) throws NoSuchMethodException {
        this.declaringClass = new WeakReference<>(declaringClass);
        this.name = name;
        parameterTypes = parameters;
        methodReference = new WeakReference<>(lookup());
    }

    public static WeakMethodReference create(Class<?> declaringClass, String name, Class<?>... parameters) throws NoSuchMethodException {
        return new WeakMethodReference(declaringClass, name, parameters);
    }

    public Method get() {
        Method method = methodReference.get();
        if (method == null) {
            try {
                method = lookup();
                if (method != null) {
                    log.trace("Renewed method reference of '{}' of class '{}'", name, method.getDeclaringClass()
                            .getName());
                    methodReference = new WeakReference<>(method);
                }
            } catch (NoSuchMethodException e) {
                log.error("Could not lookup method", e);
            }
        }
        return method;
    }

    private Method lookup() throws NoSuchMethodException {
        Class<?> declaring = declaringClass.get();
        if (declaring != null) {
            return declaring.getDeclaredMethod(name, parameterTypes);
        }
        return null;
    }

}
