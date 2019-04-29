package rocks.inspectit.ocelot.core.instrumentation.genericactions;

import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;

public class GenericActionTemplate implements IGenericAction, DoNotInstrumentMarker {

    public static final IGenericAction INSTANCE = new GenericActionTemplate();

    /**
     * This methods body will be replaced via javassist to the actual generic action code.
     */
    public static Object executeImpl(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] additionalArgs) {
        return null;
    }

    @Override
    public Object execute(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] additionalArgs) {
        return executeImpl(instrumentedMethodArgs, thiz, returnValue, thrown, additionalArgs);
    }
}
