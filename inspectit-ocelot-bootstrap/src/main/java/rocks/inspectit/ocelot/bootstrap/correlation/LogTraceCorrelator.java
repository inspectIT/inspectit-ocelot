package rocks.inspectit.ocelot.bootstrap.correlation;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Implementations of this interface are responsible for putting the active TraceID into the MDCs of all used logging library to enable log correlation.
 * Note that log-correlation is always active, it however gets disabled by using the {@link rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopLogTraceCorrelator}
 * implementation.
 */
public interface LogTraceCorrelator {

    /**
     * Executes a given function which starts a scoped span.
     * The scoped span puts the current span onto the context as active and removes it when closed.
     * <p>
     * This method observers, if the current trace on the context has changed after executing the supplied function.
     * If the trace has changed, the MDC of all logging libraries is updated to the new traceID.
     * In addition the span scoped is wrapped with a closeable which also undos these correlation changes.
     *
     * @param spanScopeStarter a function which starts (and returns) a scoped span
     * @return the return value of spanScopeStarter if no MDC update was required. Otherwise a wreapping closeable which closes the span and undos the log correlation.
     */
    AutoCloseable startCorrelatedSpanScope(Supplier<? extends AutoCloseable> spanScopeStarter);

    /**
     * Wraps the given runnable so that on execution the trace being active is PUT onto the MDC.
     *
     * @param runnable the runnable to wrap
     * @return a wrapped runnable having log correlation enabled
     */
    Runnable wrap(Runnable runnable);

    /**
     * Wraps the given callable so that on execution the trace being active is PUT onto the MDC.
     *
     * @param callable the callable to wrap
     * @return a wrapped callable having log correlation enabled
     */
    <T> Callable<T> wrap(Callable<T> callable);

    /**
     * Updates all MDCs to contain the currently active trace ID.
     * The returned AutoCloseable undos these changes.
     *
     * @return a closeable which undos the correlation changes.
     */
    AutoCloseable applyCorrelationToMDC();
}
