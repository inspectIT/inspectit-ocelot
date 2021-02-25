package rocks.inspectit.ocelot.core.instrumentation.autotracing.events;

import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.StackTrace;

/**
 * This event represents the end (=return or throw) of an instrumented and traced method.
 * A {@link MethodExitEvent} belogns to exactly one {@link MethodEntryEvent}.
 */
@Value
public class MethodExitEvent implements TraceEvent {

    /**
     * The entry event corresponding to this exit event.
     * This pair together defines the duration of the method call.
     */
    private MethodEntryEvent entryEvent;

    /**
     * The timestamp of this event.
     */
    private long timestamp;

    @Override
    public StackTrace getStackTrace() {
        return null; //exit events never have a stack trace
    }
}
