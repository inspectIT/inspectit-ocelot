package rocks.inspectit.ocelot.core.instrumentation.autotracing.events;

import io.opencensus.trace.Span;
import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.PlaceholderSpan;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.StackTrace;

/**
 * This event represents the start of an instrumented and traced method.
 * If a method is traced normally within a trace with stack trace sampling enabled,
 * special handling is required. The parent span of the method might need to be changed when the stack-trace samples are analyzed.
 * <p>
 * For this reason, no normal span is created for such methods, instead a {@link PlaceholderSpan} is used which never actually gets exported.
 * A placeholder-span however does reserve a span-id (so for example trace-id-propagation works correctly).
 * <p>
 * Every {@link MethodEntryEvent} also has a {@link MethodExitEvent} which together define the duration of the method call.
 */
@Value
public class MethodEntryEvent implements TraceEvent {

    /**
     * If this method invocation started a new span, is is stored in this variable.
     * Either {@link #placeholderSpan} or {@link #continuedSpan} are not null.
     */
    private PlaceholderSpan placeholderSpan;

    /**
     * If this method continued an existing span, the continued span is stored in this variable.
     * Either {@link #placeholderSpan} or {@link #continuedSpan} are not null.
     */
    private Span continuedSpan;

    /**
     * The time at which the method was entered.
     */
    private long timestamp;

    /**
     * The fulyl qualified name of the class declaring the method which was entered.
     */
    private String className;

    /**
     * The name of the method which was entered.
     */
    private String methodName;

    /**
     * A stack trace of all parent calls.
     * Does NOT include the method represented by the dummy span (=this invocation).
     * Instead, the last element on the stack trace is the parent of this call.
     * <p>
     * When recording this event this stackTrace is null for performance reasons:
     * Taking an additional stack trace for each method is way to expensive.
     * Instead the {@link rocks.inspectit.ocelot.core.instrumentation.autotracing.InvocationResolver}
     * will attempt to create this stackTrace based on stack-trace-samples which have fallen into this methods execution.
     */
    private StackTrace stackTrace;

    /**
     * Creates an exact copy of this event but replaces the stackTraces with the given one.
     *
     * @param stackTrace the stackTrace to use for the new event.
     *
     * @return the copy with the stack trace replaced.
     */
    public MethodEntryEvent copyWithNewStackTrace(StackTrace stackTrace) {
        return new MethodEntryEvent(placeholderSpan, continuedSpan, timestamp, className, methodName, stackTrace);
    }

}
