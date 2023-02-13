package rocks.inspectit.ocelot.core.opentelemetry.trace.samplers;

import com.google.common.annotations.VisibleForTesting;
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
 * Custom implementation of {@link Sampler} that wraps another {@link Sampler}, depending on the {@link #sampleMode}.
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
    @Getter
    private SampleMode sampleMode;

    /**
     * The sample probability.
     */
    @Getter
    private double sampleProbability = -1;

    /**
     * Creates a new {@link DynamicSampler} with the given sample probability and the {@link SampleMode#PARENT_BASED] as the default sample mode.
     *
     * @param sampleProbability The sample probability, see {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings}
     *                          and {@link Sampler#traceIdRatioBased(double)}
     */
    @VisibleForTesting
    public DynamicSampler(double sampleProbability) {
        this(SampleMode.PARENT_BASED, sampleProbability);
    }

    /**
     * Creates a new {@link DynamicSampler} with the given {@link SampleMode sample mode} and sample probability
     *
     * @param sampleMode        The {@link SampleMode}
     * @param sampleProbability The sample probability, see {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings}
     *                          and {@link Sampler#traceIdRatioBased(double)}
     */
    public DynamicSampler(SampleMode sampleMode, double sampleProbability) {
        setSampler(sampleMode, sampleProbability);
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return String.format("DynamicSampler{root:%s}", sampler.getDescription());
    }

    /**
     * Sets the {@link SampleMode sample mode} and sample probability. If the given sample probability or sample mode has changed, a new sampler
     * is created via {@link OcelotSamplerUtils#create(SampleMode, double)}.
     *
     * @param sampleMode        The {@link SampleMode}
     * @param sampleProbability The sample probability
     */
    public void setSampler(SampleMode sampleMode, double sampleProbability) {
        if (this.sampleProbability != sampleProbability || sampleMode != this.sampleMode) {
            this.sampleMode = sampleMode;
            this.sampleProbability = sampleProbability;
            sampler = OcelotSamplerUtils.create(sampleMode, sampleProbability);
        }
    }

    /**
     * Sets the {@link Sampler sampler} in case it differs from {@link #sampler}.
     *
     * @param sampler
     */
    public void setSampler(Sampler sampler) {
        if (this.sampler != sampler) {
            this.sampler = sampler;
            sampleMode = OcelotSamplerUtils.extractSampleMode(sampler);
            sampleProbability = OcelotSamplerUtils.extractSampleProbability(sampler);
        }
    }

}
