package rocks.inspectit.ocelot.core.instrumentation.actions.template;

import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GenericActionTemplate implements IGenericAction, DoNotInstrumentMarker {

    public static final IGenericAction INSTANCE = new GenericActionTemplate();

    /**
     * Global general purpose cache, which can be used within every action.
     * No input variable is required to use {@code _cache} within actions.
     */
    private static final Map<Object, Object> _cache = new ConcurrentHashMap<>();

    /**
     * This method's body will be replaced via javassist to the actual generic action code.
     */
    public static Object executeImpl(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] actionArguments) {
        return null;
    }

    @Override
    public Object execute(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, Object[] actionArguments) {
        return executeImpl(instrumentedMethodArgs, thiz, returnValue, thrown, actionArguments);
    }
}
