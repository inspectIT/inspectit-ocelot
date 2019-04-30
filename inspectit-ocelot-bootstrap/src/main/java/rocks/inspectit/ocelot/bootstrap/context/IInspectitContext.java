package rocks.inspectit.ocelot.bootstrap.context;

import rocks.inspectit.ocelot.bootstrap.accessible.InspectitContext;

/**
 * Interface abstraction for the InspectitContext.
 */
public interface IInspectitContext extends AutoCloseable, InspectitContext {

    /**
     * Special data key which stores the remote parent SpanContext if any is present.
     * As soon as a new span is opened, this parent is used and the data is cleared from the context.
     */
    String REMOTE_PARENT_SPAN_CONTEXT_KEY = "remote_parent_span_context";

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
