package rocks.inspectit.ocelot.bootstrap.context.noop;

import rocks.inspectit.ocelot.bootstrap.context.IContextManager;
import rocks.inspectit.ocelot.bootstrap.context.IInspectitContext;

import java.util.concurrent.Callable;

/**
 * No-operations implementation of the {@link IContextManager}. This will be used if there is no inspectIT agent available.
 */
public class NoopContextManager implements IContextManager {

    public static final IContextManager INSTANCE = new NoopContextManager();

    private NoopContextManager() {
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        return runnable;
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        return callable;
    }

    @Override
    public void storeContextForThread(Thread thread) {
    }

    @Override
    public void attachContextToThread(Thread thread) {
    }

    @Override
    public IInspectitContext enterNewContext() {
        return NoopContext.INSTANCE;
    }

}