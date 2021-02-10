package rocks.inspectit.ocelot.bootstrap.context;

import java.util.concurrent.Callable;

/**
 * Manages the context, meaning:
 * - the trace / span state
 * - the tags
 * - the data-set
 */
public interface IContextManager {

    /**
     * Sets a flag to the current thead that a correlation is happening which is not done yet. In case a correlation has
     * already been started on the current thread, the method will return <code>false</code>.
     *
     * @return <code>true</code> if no correlation is in progress
     */
    boolean enterCorrelation();

    /**
     * Returns the flag marking whether a correlation is in progress.
     *
     * @return <code>true</code> if a correlation is in progess.
     */
    boolean insideCorrelation();

    /**
     * Clears the flag of the current thead that a correlation is in progress.
     */
    void exitCorrelation();

    /**
     * Stores the current context related to the specified target object in a global cache. A stored context might be
     * removed by the garbage collector, thus it is not ensured that a stored context will be available for restoring!
     *
     * @param target                 the object which can be used to restore the context
     * @param invalidateAfterRestore defines whether the context should be removed from the cache once it has been restored
     */
    void storeContext(Object target, boolean invalidateAfterRestore);

    /**
     * Attaches/Restores the context related to the specified target object on the current thead. Nothing will happen in
     * case no context could be found using the target object.
     *
     * @param target the object which can be used to restore the context
     * @return returns a {@link ContextTuple} which can be used to detach the context once it is not needed anymore
     */
    ContextTuple attachContext(Object target);

    /**
     * Detaches a previously attached context.
     *
     * @param contextTuple context tuple related to the context which should be detach
     */
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
