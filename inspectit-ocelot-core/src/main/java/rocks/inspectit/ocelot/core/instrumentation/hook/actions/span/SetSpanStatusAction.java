package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
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
                Span current = Span.current();
                current.setStatus(StatusCode.ERROR);
            }
        }
    }

    @Override
    public String getName() {
        return "Span status definition";
    }
}
