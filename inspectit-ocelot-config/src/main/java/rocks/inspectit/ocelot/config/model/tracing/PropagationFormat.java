package rocks.inspectit.ocelot.config.model.tracing;

/**
 * The currently supported propagation formats.
 */
public enum PropagationFormat {

    /**
     * Using B3 headers. See: https://github.com/openzipkin/b3-propagation
     */
    B3,

    /**
     * Using TraceContext headers. See: https://www.w3.org/TR/trace-context/
     */
    TRACE_CONTEXT,

    /**
     * Using Datadog headers.
     */
    DATADOG;

}
