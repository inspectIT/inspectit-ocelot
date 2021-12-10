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
    DATADOG,

    /**
     * Using BaggageContext headers, seehttps://www.w3.org/TR/baggage/
     */
    W3C_BAGGAGE,

    /**
     * Using Jaeger HTTP format, see https://www.jaegertracing.io/docs/1.29/client-libraries/#propagation-format
     */
    JAEGER;

}
