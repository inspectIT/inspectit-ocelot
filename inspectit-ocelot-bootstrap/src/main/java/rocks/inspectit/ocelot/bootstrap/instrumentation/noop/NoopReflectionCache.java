package rocks.inspectit.ocelot.bootstrap.instrumentation.noop;

import rocks.inspectit.ocelot.bootstrap.exposed.ReflectionCache;

public class NoopReflectionCache implements ReflectionCache {

    public static final NoopReflectionCache INSTANCE = new NoopReflectionCache();

    @Override
    public Object getFieldValue(Class<?> clazz, Object instance, String fieldName) {
        return null;
    }

    @Override
    public Object invokeMethod(Class<?> clazz, Object instance, String methodName, Object... args) {
        return null;
    }
}
