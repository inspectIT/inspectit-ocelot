package rocks.inspectit.ocelot.core.opencensus;

import io.opencensus.trace.*;
import io.opencensus.trace.samplers.Samplers;

import java.util.List;

/**
 * A custom probability-based sampler which only flips a coin if the given span is a root span.
 * For non-root spans the sampling decision is always inherited.
 */
public class OcelotProbabilitySampler extends Sampler {

    private Sampler probabilitySampler;

    public OcelotProbabilitySampler(double probability) {
        probabilitySampler = Samplers.probabilitySampler(probability);
    }

    @Override
    public boolean shouldSample(SpanContext parentContext, Boolean hasRemoteParent, TraceId traceId, SpanId spanId, String name, List<Span> parentLinks) {
        if (parentContext != null && parentContext.isValid()) {
            return parentContext.getTraceOptions().isSampled();
        }
        return probabilitySampler.shouldSample(parentContext, hasRemoteParent, traceId, spanId, name, parentLinks);
    }

    @Override
    public String getDescription() {
        return "A probability sampler which always respects the parent spans sampling decision";
    }
}
