package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import lombok.Getter;

import java.util.List;

/**
 * Custom implementation of {@link Sampler} that wraps {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler}.
 * This wrapper is used to change the {@link #sampleProbability} without resetting {@link io.opentelemetry.api.GlobalOpenTelemetry#set(OpenTelemetry)}.
 * For use, create the {@link DynamicSampler} and set it once to {@link io.opentelemetry.sdk.trace.SdkTracerProviderBuilder#setSampler(Sampler)}.
 */
public class DynamicSampler implements Sampler {

    /**
     * The {@link Sampler} implementation.
     */
    private Sampler sampler;

    /**
     * The sample probability.
     */
    @Getter
    private double sampleProbability;

    /**
     * Creates a new {@link DynamicSampler} with the given sample probability
     * @param sampleProbability The sample probability, see {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings#sampleProbability} and {@link Sampler#traceIdRatioBased(double)}
     */
    public DynamicSampler(double sampleProbability) {
        this.sampleProbability = sampleProbability;
        this.sampler = Sampler.traceIdRatioBased(sampleProbability);
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return sampler.getDescription();
    }

    /**
     * Sets the sample probability. If the given sample probability does not equal {@link #sampleProbability}, a new {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler} is created via {@link Sampler#traceIdRatioBased(double)}
     *
     * @param sampleProbability The sample probability
     */
    public void setSampleProbability(double sampleProbability) {
        if (this.sampleProbability != sampleProbability) {
            this.sampleProbability = sampleProbability;
            this.sampler = Sampler.traceIdRatioBased(sampleProbability);
        }
    }

}
