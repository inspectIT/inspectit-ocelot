package rocks.inspectit.ocelot.core.opentelemetry.trace.samplers;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import rocks.inspectit.ocelot.config.model.tracing.SampleMode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to create {@link Sampler}
 */
public class OcelotSamplerUtils {

    /**
     * Creates a new {@link Sampler} for the given {@link SampleMode sample mode} and sample probability.
     *
     * @param sampleMode
     * @param sampleProbability
     *
     * @return
     */
    public static Sampler create(SampleMode sampleMode, double sampleProbability) {
        switch (sampleMode) {
            case PARENT_BASED:
                return Sampler.parentBased(Sampler.traceIdRatioBased(sampleProbability));
            case TRACE_ID_RATIO_BASED:
                return Sampler.traceIdRatioBased(sampleProbability);
            case HYBRID_PARENT_TRACE_ID_RATIO_BASED:
                return HybridParentTraceIdRatioBasedSampler.create(sampleProbability);
        }
        return null;
    }

    /**
     * Extracts the {@link SampleMode} for the given {@link Sampler}
     *
     * @param sampler
     *
     * @return
     */
    public static SampleMode extractSampleMode(Sampler sampler) {
        if (null == sampler) {
            return null;
        }
        String description = sampler.getDescription();
        if (description.contains(HybridParentTraceIdRatioBasedSampler.class.getSimpleName())) {
            return SampleMode.HYBRID_PARENT_TRACE_ID_RATIO_BASED;
        } else if (description.contains("ParentBased")) {
            return SampleMode.PARENT_BASED;
        } else if (description.contains("TraceIdRatioBased")) {
            return SampleMode.TRACE_ID_RATIO_BASED;
        }
        throw new RuntimeException("Cannot extract sample mode from " + sampler.getDescription());
    }

    /**
     * The {@link Pattern} to match for in {@link Sampler#getDescription()}. In case of {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler}, we are looking for the double in curly braces
     */
    private final static Pattern ratioPattern = Pattern.compile("TraceIdRatioBased\\{([0-9]+\\.[0-9]+)\\}");

    /**
     * Extracts the sample probability of the given {@link Sampler} contains a {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler}
     * If the provided {@code sampler} does not contain a {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler}, this method will throw a {@link RuntimeException}
     *
     * @param sampler
     *
     * @return
     */
    public static double extractSampleProbability(Sampler sampler) {
        if (sampler.getDescription().contains("TraceIdRatioBased")) {
            Matcher m = ratioPattern.matcher(sampler.getDescription());
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        }
        throw new RuntimeException("The provided sampler did not contain any TraceIdRatioBasedSampler");
    }

}
