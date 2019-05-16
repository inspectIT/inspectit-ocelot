package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import lombok.AllArgsConstructor;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContext;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

/**
 * If {@link InspectitContext#enterSpan(Span)} was invoked on the currently active context,
 * the entered span is stored in a specified context variable.
 */
@AllArgsConstructor
public class StoreSpanAction implements IHookAction {

    /**
     * The data key under which a started or continued span will be stored.
     * Must not be null.
     */
    private final String dataKey;


    @Override
    public void execute(ExecutionContext context) {
        InspectitContext ctx = context.getInspectitContext();
        if (ctx.wasSpanEntered()) {
            ctx.setData(dataKey, Tracing.getTracer().getCurrentSpan());
        }
    }

    @Override
    public String getName() {
        return "Span storing";
    }
}
