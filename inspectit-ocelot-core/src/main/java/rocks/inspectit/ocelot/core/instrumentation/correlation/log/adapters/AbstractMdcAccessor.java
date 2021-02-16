package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;

import java.lang.reflect.Method;

public abstract class AbstractMdcAccessor extends MdcAccessor {

    public abstract Method getGetMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    public abstract Method getPutMethod(Class<?> mdcClazz) throws NoSuchMethodException;

    public abstract Method getRemoveMethod(Class<?> mdcClazz) throws NoSuchMethodException;

}
