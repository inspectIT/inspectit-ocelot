package rocks.inspectit.ocelot.bootstrap.correlation;

/**
 * Implementations of this interface will inject the current trace id (if existing) into the given message. Normally,
 * the message is a log format of a logging framework.
 */
public interface TraceIdInjector {

    /**
     * Injects the trace id (if existing) into the given message and returns the result.
     *
     * @param message the message to use
     *
     * @return the resulting object of the injection
     */
    Object injectTraceId(Object message);
}
