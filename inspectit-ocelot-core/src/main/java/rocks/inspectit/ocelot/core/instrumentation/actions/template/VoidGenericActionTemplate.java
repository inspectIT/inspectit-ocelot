package rocks.inspectit.ocelot.core.instrumentation.actions.template;

import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;

public class VoidGenericActionTemplate implements IGenericAction, DoNotInstrumentMarker {

    public static final IGenericAction INSTANCE = new VoidGenericActionTemplate();

    /**
     * This methods body will be replaced via javassist to the actual generic action code.
     */
    public static void executeImpl(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] additionalArgs) {
    }

    @Override
    public Object execute(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] additionalArgs) {
        executeImpl(instrumentedMethodArgs, thiz, returnValue, thrown, additionalArgs);
        return null;
    }
}
