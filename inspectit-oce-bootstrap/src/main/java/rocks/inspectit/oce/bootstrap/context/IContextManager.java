package rocks.inspectit.oce.bootstrap.context;

import java.util.concurrent.Callable;

/**
 * Manages the context, meaning:
 * - the trace / span state
 * - the tags
 * - the data-set
 */
public interface IContextManager {

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
     * Stores the current context in relation to the given thread.
     *
     * @param thread The thread related to the current context.
     */
    void storeContextForThread(Thread thread);

    /**
     * Attaches/restores the context which is stored for the given thread to the given thread.
     *
     * @param thread The given thread which context should be restored.
     */
    void attachContextToThread(Thread thread);
}
