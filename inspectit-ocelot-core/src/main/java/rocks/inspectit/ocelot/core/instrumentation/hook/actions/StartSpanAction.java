package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.trace.Span;
import lombok.AllArgsConstructor;
import rocks.inspectit.ocelot.core.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContext;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodReflectionInformation;

/**
 * Invokes {@link InspectitContext#beginSpan(String, Span.Kind)} on the currently active context.
 */
@AllArgsConstructor
public class StartSpanAction implements IHookAction {

    /**
     * The data key to fetch from the current context whose value will then be used as name for the span.
     * Is configured using {@link RuleTracingSettings#getName()}
     * If this key is null or the assigned value is null, the method FQN will be used as span name.
     */
    private final String nameDataKey;

    /**
     * The span kind to use when opening a span, can be null.
     */
    private final Span.Kind spanKind;


    @Override
    public void execute(ExecutionContext context) {
        InspectitContext ctx = context.getInspectitContext();
        String spanName = getSpanName(ctx, context.getHook().getMethodInformation());
        ctx.beginSpan(spanName, spanKind);
    }

    @Override
    public String getName() {
        return "Span creation";
    }

    private String getSpanName(InspectitContext inspectitContext, MethodReflectionInformation methodInfo) {
        String name = null;
        if (nameDataKey != null) {
            Object data = inspectitContext.getData(nameDataKey);
            if (data != null) {
                name = data.toString();
            }
        }
        if (name == null) {
            name = methodInfo.getMethodFQN();
        }
        return name;
    }
}
