package rocks.inspectit.ocelot.core.instrumentation.dataprovider.generic;

import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericDataProvider;

public class GenericDataProviderTemplate implements IGenericDataProvider, DoNotInstrumentMarker {

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
