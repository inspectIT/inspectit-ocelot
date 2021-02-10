package rocks.inspectit.ocelot.bootstrap.context;

import java.util.concurrent.Callable;

/**
 * Manages the context, meaning:
 * - the trace / span state
 * - the tags
 * - the data-set
 */
public interface IContextManager {

    boolean enterCorrelation();

    boolean insideCorrelation();

    void exitCorrelation();

    void storeContext(Object target, boolean invalidateAfterRestore);

    ContextTuple attachContext(Object target);

    void detachContext(ContextTuple contextTuple);

    /**
     * Wraps the given runnable so that current context is used when the runnable is invoked.
     *
     * @param r the runnable to wrap
     * @return the wrapped runnable
     */
    Runnable wrap(Runnable r);

    /**
     * Wraps the given Callable so that it will attach and detach the current GRPC context during execution of its {@link Callable#call()}.
     *
     * @param callable the callable to wrap
     * @param <T>      the callable's generic type
     * @return the wrapped callable
     */
    <T> Callable<T> wrap(Callable<T> callable);

    /**
     * Creates a new context which is not yet active.
     * After the initial data collection has been performed {@link InternalInspectitContext#makeActive()} neeeds to be called
     *
     * @return the newly created context
     */
    InternalInspectitContext enterNewContext();

}
