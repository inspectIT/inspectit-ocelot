package rocks.inspectit.ocelot.core.opentelemetry.trace.samplers;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;
import java.util.Objects;

/**
 * An alternative to the {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler} that always samples if the parent span was sampled, and otherwise applies the underlying {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler root sampler}
 * This behaviour is similar to the <a href="https://github.com/census-instrumentation/opencensus-java/blob/master/api/src/main/java/io/opencensus/trace/samplers/ProbabilitySampler.java">OpenCensus ProbabilitySampler</a>.
 */
public class HybridParentTraceIdRatioBasedSampler implements Sampler {

    /**
     * The underlying {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler}
     */
    private final Sampler root;

    /**
     * Creates a new {@link HybridParentTraceIdRatioBasedSampler} with the given sample probability to used for the {@link #root}
     *
     * @param sampleProbability
     *
     * @return
     */
    public static HybridParentTraceIdRatioBasedSampler create(double sampleProbability) {
        return new HybridParentTraceIdRatioBasedSampler(sampleProbability);
    }

    private HybridParentTraceIdRatioBasedSampler(double sampleProbability) {
        root = Sampler.traceIdRatioBased(sampleProbability);
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
        // If the parent is sampled keep the sampling decision.
        if (parentSpanContext.isValid() && parentSpanContext.isSampled()) {
            return SamplingResult.recordAndSample();
        }

        boolean isAnyParentLinkSampled = isAnyParentLinkSampled(parentLinks);

        // if any parent link has been sampled, keep the sampling decision.
        if (isAnyParentLinkSampled) {
            return SamplingResult.recordAndSample();
        }

        return root.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return String.format("HybridParentTraceIdRatioBasedSampler{root:%s}", root.getDescription());
    }

    private static boolean isAnyParentLinkSampled(List<LinkData> parentLinks) {
        if (null == parentLinks) {
            return false;
        }
        for (LinkData parentLink : parentLinks) {
            if (parentLink.getSpanContext().getTraceFlags().isSampled()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HybridParentTraceIdRatioBasedSampler)) {
            return false;
        }
        HybridParentTraceIdRatioBasedSampler otherSampler = (HybridParentTraceIdRatioBasedSampler) other;
        return root.equals(otherSampler.root);
    }

    @Override
    public int hashCode() {
        return 29 * Objects.hash(root);
    }
}
