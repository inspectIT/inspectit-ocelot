package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import lombok.AllArgsConstructor;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import java.util.function.Predicate;

/**
 * Ends the current span if the specified conditions are met and {@link InspectitContextImpl#enterSpan(Span)} was invoked on active context.
 */
@AllArgsConstructor
public class EndSpanAction implements IHookAction {

    private Predicate<ExecutionContext> condition;

    @Override
    public void execute(ExecutionContext context) {
        InspectitContextImpl ctx = context.getInspectitContext();
        if (ctx.hasEnteredSpan() && condition.test(context)) {
            Span current = Tracing.getTracer().getCurrentSpan();
            current.end();
        }
    }

    @Override
    public String getName() {
        return "Span ending";
    }
}
