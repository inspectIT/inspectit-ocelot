package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.trace.Span;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import lombok.AllArgsConstructor;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodReflectionInformation;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import java.util.function.Predicate;

/**
 * Invokes {@link InspectitContextImpl#enterSpan(Span)} on the currently active context.
 */
@AllArgsConstructor
public class ContinueOrStartSpanAction implements IHookAction {

    /**
     * The data key to fetch from the current context whose value will then be used as name for a newly began span.
     * Is configured using {@link RuleTracingSettings#getName()}
     * If this key is null or the assigned value is null, the methods FQN without the package will be used as span name.
     */
    private final String nameDataKey;

    /**
     * The span kind to use when beginning a new span, can be null.
     */
    private final Span.Kind spanKind;

    /**
     * The data key to read for continuing a span.
     */
    private final String continueSpanDataKey;

    /**
     * The condition which defines if this actions attempts to continue the span defined by {@link #continueSpanDataKey}.
     * An attempt to continue a span has higher priority than an attempt to start a span.
     */
    private Predicate<ExecutionContext> continueSpanCondition;

    /**
     * The condition which defines if this actions attempts to start a new span.
     * An attempt to start a new span is only made if no span was continued.
     */
    private Predicate<ExecutionContext> startSpanCondition;


    @Override
    public void execute(ExecutionContext context) {
        InspectitContextImpl ctx = context.getInspectitContext();
        if (continueSpanDataKey != null && continueSpanCondition.test(context)) {
            Object spanObj = ctx.getData(continueSpanDataKey);
            if (spanObj instanceof Span) {
                ctx.enterSpan((Span) spanObj);
                return;
            }
        }
        //no span was continued, attempt to create a new (if configured..)
        if (startSpanCondition.test(context)) {
            String spanName = getSpanName(ctx, context.getHook().getMethodInformation());
            SpanContext remoteParent = ctx.getAndClearCurrentRemoteSpanContext();
            SpanBuilder builder;
            if (remoteParent != null) {
                builder = Tracing.getTracer().spanBuilderWithRemoteParent(spanName, remoteParent);
            } else {
                builder = Tracing.getTracer().spanBuilder(spanName);
            }
            builder.setSpanKind(spanKind);
            builder.setSampler(Samplers.alwaysSample());

            ctx.enterSpan(builder.startSpan());
        }
    }

    @Override
    public String getName() {
        return "Span continuing / creation";
    }

    private String getSpanName(InspectitContextImpl inspectitContext, MethodReflectionInformation methodInfo) {
        String name = null;
        if (nameDataKey != null) {
            Object data = inspectitContext.getData(nameDataKey);
            if (data != null) {
                name = data.toString();
            }
        }
        if (name == null) {
            name = methodInfo.getDeclaringClass().getSimpleName() + "." + methodInfo.getName();
        }
        return name;
    }
}
