package rocks.inspectit.ocelot.bootstrap.instrumentation.noop;

import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.bootstrap.context.noop.NoopContext;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IMethodHook;

public class NoopMethodHook implements IMethodHook {

    public static final NoopMethodHook INSTANCE = new NoopMethodHook();

    private NoopMethodHook() {
    }

    @Override
    public InternalInspectitContext onEnter(Object[] instrumentedMethodArgs, Object thiz) {
        return NoopContext.INSTANCE;
    }

    @Override
    public void onExit(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, InternalInspectitContext context) {
    }
}
