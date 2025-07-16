package rocks.inspectit.ocelot.bootstrap.instrumentation.noop;

import rocks.inspectit.ocelot.bootstrap.exposed.InspectitReflection;

public class NoopInspectitReflection implements InspectitReflection {

    public static final NoopInspectitReflection INSTANCE = new NoopInspectitReflection();

    @Override
    public Object getFieldValue(Class<?> clazz, Object instance, String fieldName) {
        return null;
    }

    @Override
    public Object invokeMethod(Class<?> clazz, Object instance, String methodName, Object... args) {
        return null;
    }
}
