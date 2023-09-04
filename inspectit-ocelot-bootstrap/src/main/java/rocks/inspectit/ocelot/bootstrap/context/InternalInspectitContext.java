package rocks.inspectit.ocelot.bootstrap.context;

import rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext;

/**
 * Interface abstraction for the InspectitContext.
 */
public interface InternalInspectitContext extends AutoCloseable, InspectitContext {

    /**
     * Special data key which stores the remote parent SpanContext if any is present.
     * As soon as a new span is opened, this parent is used and the data is cleared from the context.
     */
    String REMOTE_PARENT_SPAN_CONTEXT_KEY = "remote_parent_span_context";

    /**
     * Special data key which stores the remote session id if any is present
     * The remains the same within one trace. Usually the data key will be down-propagated.
     * The data will be cleared as soon as the root span is closed
     */
    String REMOTE_SESSION_ID = "remote_session_id";

    /**
     * Special data key which stores the trace context of the current inspectit context.
     * The trace context will be set after the current span was created
     */
    String TRACEPARENT = "traceparent";

    /**
     * Makes this context the active one.
     * This means all new contexts created from this point will use this context as a parent.
     * Async context will see down-propagated data in the state it was when this method was calle.
     */
    void makeActive();

    /**
     * Closes this context, performing up-propagation if required.
     */
    @Override
    void close();
}
