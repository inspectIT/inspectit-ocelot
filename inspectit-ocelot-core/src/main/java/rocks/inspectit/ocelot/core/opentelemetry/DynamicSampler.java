package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import lombok.Getter;
import rocks.inspectit.ocelot.config.model.tracing.SampleMode;

import java.util.List;

/**
 * Custom implementation of {@link Sampler} that either wraps a "parent-based" sampler with default "trace-id-ratio" sampler or just a "trace-id-ratio-based" sampler,
 * depending on the {@link #sampleMode}.
 * In case of {@link SampleMode#PARENT_BASED}, it means that the sampling decision of a parent span is used. If no parent exists, the decision is delegated to the ratio sampler.
 * This wrapper is used to change the {@link #sampleProbability} without resetting {@link io.opentelemetry.api.GlobalOpenTelemetry#set(OpenTelemetry)}.
 * For use, create the {@link DynamicSampler} and set it once to {@link io.opentelemetry.sdk.trace.SdkTracerProviderBuilder#setSampler(Sampler)}.
 */
public class DynamicSampler implements Sampler {

    /**
     * The {@link Sampler} implementation.
     */
    private Sampler sampler;

    /**
     * The default {@link SampleMode}
     */
    private SampleMode defaultSampleMode;

    /**
     * The sample probability.
     */
    @Getter
    private double sampleProbability = -1;

    /**
     * Creates a new {@link DynamicSampler} with the given sample probability and the {@code SampleMode#PARENT_BASED} as the default sample mode.
     *
     * @param sampleProbability The sample probability, see {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings}
     *                          and {@link Sampler#traceIdRatioBased(double)}
     */
    public DynamicSampler(double sampleProbability) {
        setSampleProbability(SampleMode.PARENT_BASED, sampleProbability);
    }

    /**
     * Creates a new {@link DynamicSampler} with the given sample probability
     *
     * @param defaultSampleMode The default {@link SampleMode}
     * @param sampleProbability The sample probability, see {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings}
     *                          and {@link Sampler#traceIdRatioBased(double)}
     */
    public DynamicSampler(SampleMode defaultSampleMode, double sampleProbability) {
        setSampleProbability(defaultSampleMode, sampleProbability);
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
     * Sets the sample probability. If the given sample probability does not equal {@link #sampleProbability}, a new sampler
     * is created via {@link Sampler#traceIdRatioBased(double)} and {@link Sampler#parentBased(Sampler)}.
     *
     * @param sampleProbability The sample probability
     */
    public void setSampleProbability(SampleMode defaultSampleMode, double sampleProbability) {
        if (this.sampleProbability != sampleProbability || defaultSampleMode != this.defaultSampleMode) {
            this.defaultSampleMode = defaultSampleMode;
            this.sampleProbability = sampleProbability;
            switch (defaultSampleMode) {
                case PARENT_BASED:
                    sampler = Sampler.parentBased(Sampler.traceIdRatioBased(sampleProbability));
                    break;
                case TRACE_ID_RATIO_BASED:
                    sampler = Sampler.traceIdRatioBased(sampleProbability);
                    break;
            }
        }
    }

}
