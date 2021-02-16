package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import java.lang.reflect.Method;

public class Slf4JMdcAccessor extends AbstractMdcAccessor {

    public static final String MDC_CLASS = "org.slf4j.MDC";

    @Override
    public Method getGetMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("get", String.class);
    }

    @Override
    public Method getPutMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("put", String.class, String.class);
    }

    @Override
    public Method getRemoveMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("remove", String.class);
    }

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public void put(String key, Object value) {
    }

    @Override
    public void remove(String key) {
    }
}
