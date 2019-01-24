package rocks.inspectit.oce.bootstrap.context;

/**
 * Manages the context, meaning:
 * - the trace / span state
 * - the tags
 * - the data-set
 */
public interface ContextManager {

    /**
     * Wraps the given runnable so that current context is used when the runnable is invoked.
     *
     * @param r the runnable to wrap
     * @return the wrapped runnable
     */
    Runnable wrap(Runnable r);

    void storeContextForThread(Thread thread);

    void attachContextToThread(Thread thread);
}
