package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.trace.*;
import io.opencensus.trace.samplers.Samplers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodReflectionInformation;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import java.util.function.Predicate;

/**
 * Invokes {@link InspectitContextImpl#enterSpan(Span)} on the currently active context.
 */
@AllArgsConstructor
@Builder
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
     * If the sample probability is fixed, this attribute holds the corresponding sampler.
     * If a dynamic sample probability is used, this value is null and {@link #dynamicSampleProbabilityKey} is not null.
     */
    private final Sampler staticSampler;

    /**
     * If the sample probability is dynamic, this attribute holds the datakey under which the sample probability is looked up from the context.
     * If no dynamic sample probability is used, {@link #staticSampler} has to be not null.
     */
    private final String dynamicSampleProbabilityKey;

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
    public String getName() {
        return "Span continuing / creation";
    }

    @Override
    public void execute(ExecutionContext context) {
        if (!continueSpan(context)) {
            startSpan(context);
        }
    }


    private boolean continueSpan(ExecutionContext context) {
        if (continueSpanDataKey != null && continueSpanCondition.test(context)) {
            InspectitContextImpl ctx = context.getInspectitContext();
            Object spanObj = ctx.getData(continueSpanDataKey);
            if (spanObj instanceof Span) {
                ctx.enterSpan((Span) spanObj);
                return true;
            }
        }
        return false;
    }

    private void startSpan(ExecutionContext context) {
        if (startSpanCondition.test(context)) {
            InspectitContextImpl ctx = context.getInspectitContext();

            Sampler sampler = staticSampler;
            if (sampler == null) {
                Object probability = ctx.getData(dynamicSampleProbabilityKey);
                if (probability instanceof Number) {
                    sampler = Samplers.probabilitySampler(Math.min(1, Math.max(0, ((Number) probability).doubleValue())));
                } else {
                    sampler = Samplers.neverSample();
                }
            }

            String spanName = getSpanName(ctx, context.getHook().getMethodInformation());
            SpanContext remoteParent = ctx.getAndClearCurrentRemoteSpanContext();
            SpanBuilder builder;
            if (remoteParent != null) {
                builder = Tracing.getTracer().spanBuilderWithRemoteParent(spanName, remoteParent);
            } else {
                builder = Tracing.getTracer().spanBuilder(spanName);
            }
            builder.setSpanKind(spanKind);
            builder.setSampler(sampler);

            ctx.enterSpan(builder.startSpan());
        }
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
