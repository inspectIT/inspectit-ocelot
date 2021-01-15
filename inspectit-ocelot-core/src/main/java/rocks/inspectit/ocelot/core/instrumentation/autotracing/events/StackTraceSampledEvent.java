package rocks.inspectit.ocelot.core.instrumentation.autotracing.events;

import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.StackTrace;

/**
 * This event represents a captured stack-trace of the target thread at a given timestamp.
 */
@Value
public class StackTraceSampledEvent implements TraceEvent {

    /**
     * The captured stack trace.
     */
    private StackTrace stackTrace;

    /**
     * The time at which the stack trace was captured.
     */
    private long timestamp;

}
