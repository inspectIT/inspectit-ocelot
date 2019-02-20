package rocks.inspectit.oce.bootstrap.instrumentation.noop;

import rocks.inspectit.oce.bootstrap.instrumentation.IHookManager;
import rocks.inspectit.oce.bootstrap.instrumentation.IMethodHook;


public class NoopHookManager implements IHookManager {

    public static final NoopHookManager INSTANCE = new NoopHookManager();

    private NoopHookManager() {
    }

    @Override
    public IMethodHook getHook(Class<?> clazz, String methodSignature) {
        return NoopMethodHook.INSTANCE;
    }
}
