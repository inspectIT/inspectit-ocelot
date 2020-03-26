package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Status;
import io.opencensus.trace.Tracing;
import lombok.AllArgsConstructor;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

/**
 * Marks the span as error in case the provided variable is neither null nor false.
 * Has to be executed before the {@link EndSpanAction}.
 */
@AllArgsConstructor
public class SetSpanStatusAction implements IHookAction {

    /**
     * The variable used to decide whether this span is marked as an error or not.
     * If the value is neither null, nor false the span will be marked as an error.
     */
    private VariableAccessor errorStatus;

    @Override
    public void execute(ExecutionContext context) {
        InspectitContextImpl ctx = context.getInspectitContext();
        if (ctx.hasEnteredSpan()) {
            Object statusValue = errorStatus.get(context);
            if (statusValue != null && !Boolean.FALSE.equals(statusValue)) {
                Span current = Tracing.getTracer().getCurrentSpan();
                current.setStatus(Status.UNKNOWN);
                // Jaeger expects an "error" tag with the value "true" in case of an error, which is currently not
                // exposed by the Jaeger exporter. Therefore we manually set this attribute here.
                // As soon as it is supported by the exporter, this can be removed
                current.putAttribute("error", AttributeValue.booleanAttributeValue(true));
            }
        }
    }

    @Override
    public String getName() {
        return "Span status definition";
    }
}
