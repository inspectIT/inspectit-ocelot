package rocks.inspectit.oce.bootstrap.instrumentation.noop;

import rocks.inspectit.oce.bootstrap.context.IInspectitContext;
import rocks.inspectit.oce.bootstrap.context.noop.NoopContext;
import rocks.inspectit.oce.bootstrap.instrumentation.IMethodHook;

public class NoopMethodHook implements IMethodHook {

    public static NoopMethodHook INSTANCE = new NoopMethodHook();

    @Override
    public IInspectitContext onEnter(Object[] instrumentedMethodArgs, Object thiz) {
        return NoopContext.INSTANCE;
    }

    @Override
    public void onExit(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, IInspectitContext context) {
    }
}
