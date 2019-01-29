package rocks.inspectit.oce.core.instrumentation.dataprovider.generic;

public class GenericDataProviderTemplate implements IGenericDataProvider {

    public static final IGenericDataProvider INSTANCE = new GenericDataProviderTemplate();

    /**
     * This methods body will be replaced via javassist to the actual data provider code.
     */
    public static Object executeImpl(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] additionalArgs) {
        return null;
    }

    @Override
    public Object execute(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] additionalArgs) {
        return executeImpl(instrumentedMethodArgs, thiz, returnValue, thrown, additionalArgs);
    }
}
