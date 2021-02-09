package rocks.inspectit.ocelot.core.instrumentation.autotracing.events;

import rocks.inspectit.ocelot.core.instrumentation.autotracing.StackTrace;

/**
 * Base interface for all events required for the reconstruction of a trace from stack-trace samples and instrumented method executions.
 */
public interface TraceEvent {

    /**
     * Returns the stack trace element at the given depth counted from the root method call (e.g. Thread.run()).
     * <p>
     * If this event does not have a stack trace ({@link #getStackTrace()} returns null)
     * or the stack trace is shorter than the given depth, null is returned.
     *
     * @param depth the depth at which the {@link StackTraceElement} shall be queried, counted from the root method.
     *
     * @return the element if available, otherwise null.
     */
    default StackTraceElement getStackTraceElementAt(int depth) {
        StackTrace st = getStackTrace();
        if (st != null && st.size() > depth) {
            return st.get(depth);
        }
        return null;
    }

    /**
     * If this event has a stack-trace associated, it is returned.
     * Otherwise null.
     *
     * @return the stack trace or null if unavailable.
     */
    StackTrace getStackTrace();

    /**
     * @return the time at which this event occured.
     */
    long getTimestamp();
}
