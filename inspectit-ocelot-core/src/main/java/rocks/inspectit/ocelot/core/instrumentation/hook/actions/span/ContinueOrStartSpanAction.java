package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.trace.*;
import io.opencensus.trace.samplers.Samplers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodReflectionInformation;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.tags.CommonTagsToAttributesAction;

import java.util.function.Predicate;

/**
 * Invokes {@link InspectitContextImpl#enterSpan(Span)} on the currently active context.
 */
@AllArgsConstructor
@Builder
public class ContinueOrStartSpanAction implements IHookAction {

    /**
     * The variable accessor used to fetch the name for a newly began span.
     * Is configured using {@link RuleTracingSettings#getName()}
     * If this field is null or the returned value is null, the methods FQN without the package will be used as span name.
     */
    private final VariableAccessor nameAccessor;

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
     * If a dynamic sample probability is used, this value is null and {@link #dynamicSampleProbabilityAccessor} is not null.
     * If both {@link #staticSampler} and {@link #dynamicSampleProbabilityAccessor} are null, no span-scoped sampler will be used.
     */
    private final Sampler staticSampler;

    /**
     * If the sample probability is dynamic, this attribute holds the accessor under which the sample probability is looked up from the context.
     * It must either be null or a valid accessor.
     * If no dynamic sample probability is used, {@link #staticSampler} is used if it is not null.
     * If both {@link #staticSampler} and {@link #dynamicSampleProbabilityAccessor} are null, no span-scoped sampler will be used.
     */
    private final VariableAccessor dynamicSampleProbabilityAccessor;

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

    /**
     * Action that optionally adds common tags to the newly started span.
     */
    private CommonTagsToAttributesAction commonTagsToAttributesAction;

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
            // resolve span name
            InspectitContextImpl ctx = context.getInspectitContext();
            String spanName = getSpanName(context, context.getHook().getMethodInformation());

            // load remote parent if it exist
            SpanContext remoteParent = ctx.getAndClearCurrentRemoteSpanContext();
            boolean hasLocalParent = false;

            // create builder based on the remote parent availability
            SpanBuilder builder;
            if (remoteParent != null) {
                builder = Tracing.getTracer().spanBuilderWithRemoteParent(spanName, remoteParent);
            } else {
                // TODO is there better way of checking ctx.hasEnteredSpan();
                //  to avoid this stupid BlankSpan impl
                final Span currentSpan = Tracing.getTracer().getCurrentSpan();
                hasLocalParent = currentSpan != null && !BlankSpan.INSTANCE.equals(currentSpan);
                builder = Tracing.getTracer().spanBuilder(spanName);
            }

            // resolve kind and sampler
            builder.setSpanKind(spanKind);
            Sampler sampler = getSampler(context);
            if (sampler != null) {
                builder.setSampler(sampler);
            }

            // start span and add common tags
            final Span span = builder.startSpan();
            commonTagsToAttributesAction.writeCommonTags(span, remoteParent != null, hasLocalParent);

            // enter in the our context
            ctx.enterSpan(span);
        }
    }

    /**
     * If configured, returns a span-scoped sampler to set for the newly created span.
     * This can be either {@link #staticSampler} if a constant sampling probability was specified,
     * or a probability read from {@link InspectitContext} for a given data-key.
     * If neither is specified, null will be returned.
     *
     * @param context the context used to query a dynamic probability
     */
    @VisibleForTesting
    Sampler getSampler(ExecutionContext context) {
        Sampler sampler = staticSampler;
        if (dynamicSampleProbabilityAccessor != null) {
            Object probability = dynamicSampleProbabilityAccessor.get(context);
            if (probability instanceof Number) {
                sampler = Samplers.probabilitySampler(Math.min(1, Math.max(0, ((Number) probability).doubleValue())));
            }
        }
        return sampler;
    }

    private String getSpanName(ExecutionContext context, MethodReflectionInformation methodInfo) {
        String name = null;
        if (nameAccessor != null) {
            Object data = nameAccessor.get(context);
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
