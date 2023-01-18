package rocks.inspectit.ocelot.config.model.tracing;

/**
 * The sample mode used as the default sampling for the {@link DynamicSampler}
 */
public enum SampleMode {

    PARENT_BASED, TRACE_ID_RATIO_BASED,
    /**
     * Hybrid that always samples if the parent has been sampled, otherwise falls back to a {@link TraceIdRatioBasedSampler}. See <a href="https://github.com/census-instrumentation/opencensus-java/blob/master/api/src/main/java/io/opencensus/trace/samplers/ProbabilitySampler.java">OpenCensus ProbabilitySampler</a>.
     */
    HYBRID_PARENT_TRACE_ID_RATIO_BASED
}
