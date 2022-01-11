package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import lombok.Data;

import java.util.List;

/**
 * A custom {@link Sampler} wrapper that can be dynamically enabled or disabled.
 */
@Data
public class DynamicallyActivatableSampler implements Sampler {

    private boolean enabled = true;

    /**
     * The real implementation of the {@link Sampler}
     */
    private Sampler sampler;


    private DynamicallyActivatableSampler(Sampler sampler) {
        this.sampler = sampler;
    }

    /**
     * Creates a new {@link DynamicallyActivatableSampler} for the given {@link Sampler} implementation
     * @param sampler The underlying {@link Sampler} implementation
     */
    public static DynamicallyActivatableSampler create(Sampler sampler){
        return new DynamicallyActivatableSampler(sampler);
    }

    public static DynamicallyActivatableSampler createAlwaysOn() {
        return new DynamicallyActivatableSampler(Sampler.alwaysOn());
    }

    public static DynamicallyActivatableSampler createAlwaysOff() {
        return new DynamicallyActivatableSampler(Sampler.alwaysOff());
    }

    public static DynamicallyActivatableSampler createParentBased(Sampler root) {
        return new DynamicallyActivatableSampler(Sampler.parentBased(root));
    }

    /**
     * Returns a new {@link DynamicallyActivatableSampler} with a {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler} implementation
      * @param ratio The desired ratio of sampling. Must be within [0.0, 1.0].
     * @return A new {@link DynamicallyActivatableSampler} with a {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler} implementation.
     */
    public static DynamicallyActivatableSampler createRatio(double ratio) {
        return new DynamicallyActivatableSampler(Sampler.traceIdRatioBased(ratio));
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {

        // when enabled, call the 'real' sampler's shouldSample method
        if (isEnabled()) {
            return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
        }
        // otherwise, call the method of the alwaysOff() sampler
        else {
            return Sampler.alwaysOff().shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
        }
    }

    @Override
    public String getDescription() {
        return String.format("Custom {}", sampler.getDescription());
    }
}
