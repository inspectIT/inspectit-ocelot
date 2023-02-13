package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opentelemetry.api.trace.Span;
import lombok.AllArgsConstructor;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

/**
 * If {@link InspectitContextImpl#setSpanScope(AutoCloseable)}  was invoked on the currently active context,
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
        InspectitContextImpl ctx = context.getInspectitContext();
        if (ctx.hasEnteredSpan()) {
            ctx.setData(dataKey, Span.current());
        }
    }

    @Override
    public String getName() {
        return "Span storing";
    }
}
