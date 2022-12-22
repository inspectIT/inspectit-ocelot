package rocks.inspectit.ocelot.config.model.tracing;

/**
 * The sample mode used as the default sampling for the {@link DynamicSampler}
 */
public enum SampleMode {
    PARENT_BASED, TRACE_ID_RATIO_BASED
}
