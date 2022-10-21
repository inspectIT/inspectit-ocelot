package rocks.inspectit.ocelot.core.instrumentation.actions.template;

import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VoidGenericActionTemplate implements IGenericAction, DoNotInstrumentMarker {

    public static final IGenericAction INSTANCE = new VoidGenericActionTemplate();

    private static final Map<Object, Object> _cache = new ConcurrentHashMap<>();

    /**
     * This methods body will be replaced via javassist to the actual generic action code.
     */
    public static void executeImpl(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] actionArguments) {
    }

    @Override
    public Object execute(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] actionArguments) {
        executeImpl(instrumentedMethodArgs, thiz, returnValue, thrown, actionArguments);
        return null;
    }
}
