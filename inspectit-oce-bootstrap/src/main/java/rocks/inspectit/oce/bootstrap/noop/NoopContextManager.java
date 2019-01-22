package rocks.inspectit.oce.bootstrap.noop;

import rocks.inspectit.oce.bootstrap.context.ContextManager;

import java.util.concurrent.Callable;

/**
 * No-operations implementation of the {@link ContextManager}. This will be used if there is no inspectIT agent available.
 */
public class NoopContextManager implements ContextManager {

    public static final ContextManager INSTANCE = new NoopContextManager();

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
}