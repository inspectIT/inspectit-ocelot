package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import rocks.inspectit.ocelot.config.model.tracing.PropagationFormat;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.context.session.SessionIdManager;

import java.util.*;

/**
 * Singleton, which implements the logic for generating and reading the http headers related to context propagation.
 */
public class ContextPropagation {

    private static ContextPropagation instance;

    private final BaggagePropagation baggagePropagation;

    private final TraceContextPropagation traceContextPropagation;

    private final Set<String> PROPAGATION_FIELDS = new HashSet<>();

    /**
     * HTTP header to read session ids. Defaults to {@code Session-Id}.
     * Will be set via {@link SessionIdManager}
     */
    private String sessionIdHeader = "Session-Id";

    // Private constructor for singleton
    private ContextPropagation() {
        this.baggagePropagation = new BaggagePropagation();
        this.traceContextPropagation = new TraceContextPropagation();
        addPropagationFields();
    }

    private void addPropagationFields() {
        // We could try to use the W3CBaggagePropagator for baggage like the W3CTraceContextPropagator for traces
        PROPAGATION_FIELDS.add(BaggagePropagation.BAGGAGE_HEADER);
        PROPAGATION_FIELDS.addAll(B3Propagator.injectingSingleHeader().fields());
        PROPAGATION_FIELDS.addAll(B3Propagator.injectingMultiHeaders().fields());
        PROPAGATION_FIELDS.addAll(W3CTraceContextPropagator.getInstance().fields());
        PROPAGATION_FIELDS.addAll(DatadogFormat.INSTANCE.fields());
    }

    /**
     * @return the singleton instance of {@link ContextPropagation}
     */
    public static ContextPropagation get() {
        if (instance == null) instance = new ContextPropagation();
        return instance;
    }

    /**
     * Takes the given key-value pairs and encodes them into the Baggage header.
     *
     * @param dataToPropagate the key-value pairs to propagate.
     *
     * @return the result propagation map
     */
    public Map<String, String> buildPropagationHeaderMap(Map<String, Object> dataToPropagate) {
        return buildPropagationHeaderMap(dataToPropagate, null);
    }

    /**
     * Takes the given key-value pairs and the span context and encodes them into the Baggage header.
     *
     * @param dataToPropagate the key-value pairs to propagate.
     * @param spanToPropagate the span context to propagate, null if none shall be propagated
     *
     * @return the result propagation map
     */
    public Map<String, String> buildPropagationHeaderMap(Map<String, Object> dataToPropagate, SpanContext spanToPropagate) {
        Map<String, String> result = traceContextPropagation.buildPropagationHeaderMap(spanToPropagate);

        Map<String, String> baggage = baggagePropagation.buildBaggageHeaderMap(dataToPropagate);
        if (!baggage.isEmpty()) result.putAll(baggage);

        return result;
    }

    /**
     * Returns all header names which can potentially be output by {@link #buildPropagationHeaderMap(Map, SpanContext)}.
     *
     * @return the set of header names
     */
    public Set<String> getPropagationHeaderNames() {
        return PROPAGATION_FIELDS;
    }

    /**
     * Decodes the given header to value map into the given target context.
     *
     * @param propagationMap the headers to decode
     * @param target         the context in which the decoded data key-value pairs will be stored.
     */
    public void readPropagatedDataFromHeaderMap(Map<String, String> propagationMap, InspectitContextImpl target) {
        baggagePropagation.readPropagatedDataFromHeaderMap(propagationMap, target);
    }

    /**
     * Decodes a span context from the given header to value map into the given target context.
     *
     * @param propagationMap the headers to decode
     *
     * @return the {@code SpanContext} if the data contained any trace correlation, {@code null} otherwise.
     */
    public SpanContext readPropagatedSpanContextFromHeaderMap(Map<String, String> propagationMap) {
       return traceContextPropagation.readPropagatedSpanContextFromHeaderMap(propagationMap);
    }

    /**
     * Sets the currently used propagation format to the specified one.
     *
     * @param format the format to use
     */
    public void setPropagationFormat(PropagationFormat format) {
        this.traceContextPropagation.setPropagationFormat(format);
    }

    /**
     * Reads the session-id from the map with the current session-id-key
     *
     * @param propagationMap the headers to decode
     *
     * @return session-id if existing, otherwise null
     */
    public String readPropagatedSessionIdFromHeaderMap(Map<String,String> propagationMap) {
        return propagationMap.get(sessionIdHeader);
    }

    /**
     * Updates the current session-id-header used for storing tags for a certain time in sessions.
     *
     * @param sessionIdHeader new session-id-header
     */
    public void setSessionIdHeader(String sessionIdHeader) {
        PROPAGATION_FIELDS.remove(this.sessionIdHeader);
        this.sessionIdHeader = sessionIdHeader;
        PROPAGATION_FIELDS.add(this.sessionIdHeader);
    }
}
