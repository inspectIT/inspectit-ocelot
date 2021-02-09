package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opencensus.trace.Span;
import lombok.Getter;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.events.TraceEvent;

import java.util.Iterator;

/**
 * After a trace is reconstructed from a lsit of {@link TraceEvent},
 * the individual method calls are represented by an {@link Invocation} each.
 * <p>
 * Afterwards, only a subset of {@link Invocation} are exported as actual spans,
 * based on the {@link #isHidden()} value.
 */
public class Invocation {

    /**
     * The event defining when this method invocation started.
     * The timestamp of the event corresponds to point in time when the invocation was started.
     */
    @Getter
    private TraceEvent start;

    /**
     * The event defining when this method invocation ended.
     * The timestamp of the event corresponds to point in time when the invocation was finished.
     */
    @Getter
    private TraceEvent end;

    /**
     * If this method invocation was reconstructed from stack-trace-samples,
     * this field stores a stack-trace element identifying this method.
     * It is guaranteed that either {@link #sampledMethod}, {@link #placeholderSpan} or {@link #continuedSpan} are not null.
     */
    @Getter
    private StackTraceElement sampledMethod;

    /**
     * If this method invocation was recorded due to instrumentation AND a new span was started for this
     * invocation, it is stored in this field.
     * It is guaranteed that either {@link #sampledMethod}, {@link #placeholderSpan} or {@link #continuedSpan} are not null.
     */
    @Getter
    private PlaceholderSpan placeholderSpan;

    /**
     * If this method invocation was recorded due to instrumentation AND an existing span was continued,
     * it is stored in this field.
     * It is guaranteed that either {@link #sampledMethod}, {@link #placeholderSpan} or {@link #continuedSpan} are not null.
     */
    @Getter
    private Span continuedSpan;

    //------Linked List of children:--------------

    /**
     * Stored the invocation which happened directly after this invocation.
     * This implies that nextSibling has the same parent.
     * This field acts as a single linked list pointer for the list of children of the parent.
     */
    private Invocation nextSibling = null;

    /**
     * Pointer to the start of the linked list of children.
     */
    @Getter
    private Invocation firstChild = null;

    /**
     * Pointer to the end of the linked list of children.
     */
    @Getter
    private Invocation lastChild = null;

    /**
     * Pointer to the parent of this Invocation, if it has any.
     */
    @Getter
    private Invocation parent = null;

    /**
     * Constructs a Invocation representing an instrumented method call.
     * Exactly one of placeholderSpan or continuedSpan must not be null.
     *
     * @param start           the event when the call started
     * @param end             the event when the call ended
     * @param placeholderSpan if this method started a new span, this is its placeholder.
     * @param continuedSpan   if this method continued an existing span, this is the span which was continued.
     */
    public Invocation(TraceEvent start, TraceEvent end, PlaceholderSpan placeholderSpan, Span continuedSpan) {
        this.start = start;
        this.end = end;
        this.placeholderSpan = placeholderSpan;
        this.continuedSpan = continuedSpan;
    }

    /**
     * Construct a Invocation representing a method call which was reconstructed based on stack trace samples.
     *
     * @param start  the start of this method call
     * @param end    the end of this method call.
     * @param method
     */
    public Invocation(TraceEvent start, TraceEvent end, StackTraceElement method) {
        this.start = start;
        this.end = end;
        sampledMethod = method;
    }

    /**
     * Appends the given invocation to the end of the list of children.
     *
     * @param child the child to add.
     */
    public void addChild(Invocation child) {
        child.parent = this;
        if (firstChild == null) {
            firstChild = child;
        }
        if (lastChild != null) {
            lastChild.nextSibling = child;
        }
        lastChild = child;
    }

    public Iterable<Invocation> getChildren() {
        return () -> new ChildIterator(firstChild);
    }

    /**
     * To preserve clarity of exported traces, we "hide" certain invocations, meaning that they will not be exported.
     * We hide invocation which fullfill all of the following conditions:
     * - the invocation was generated based on stack trace samples (and NOT via instrumentation!)
     * - the invocation has exactly one child-invocation
     * - the start and end of the invocation matches it's only child
     *
     * @return true, if this invocaiton should be hidden when exported.
     */
    public boolean isHidden() {
        if (sampledMethod == null) {
            return false;
        }
        //must have exactly one child
        if (firstChild == null || firstChild != lastChild) {
            return false;
        }
        if (firstChild.start != start || firstChild.end != end) {
            return false;
        }
        return true;
    }

    /**
     * Iterator for iterating over the linked list of children.
     */
    private static class ChildIterator implements Iterator<Invocation> {

        private Invocation current;

        ChildIterator(Invocation first) {
            current = first;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Invocation next() {
            Invocation next = current;
            current = current.nextSibling;
            return next;
        }
    }

}
