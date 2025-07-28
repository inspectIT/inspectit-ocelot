package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.tracing.PropagationFormat;
import rocks.inspectit.ocelot.core.opentelemetry.trace.CustomIdGenerator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * For context propagation, when tracing is added, additional header formats such as B3 will be used.
 */
@Slf4j
public class TraceContextPropagation {

    /**
     * The currently used propagation format. Defaults to {@link W3CTraceContextPropagator}.
     * Will be set via {@link PropagationFormatManager}
     */
    private TextMapPropagator propagationFormat = W3CTraceContextPropagator.getInstance();

    public static final TextMapSetter<Map<String, String>> MAP_INJECTOR = new TextMapSetter<Map<String, String>>() {
        @Override
        public void set(Map<String, String> carrier, String key, String value) {
            carrier.put(key, value);
        }
    };

    public static final TextMapGetter<Map<String, String>> MAP_EXTRACTOR = new TextMapGetter<Map<String, String>>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };

    TraceContextPropagation() {}

    /**
     * @return the fields that will be used to read trace-context propagation data
     */
    public List<String> fields() {
        List<String> datadogFields = DatadogFormat.INSTANCE.fields();
        List<String> b3Fields = B3Format.INSTANCE.fields();
        Collection<String> traceContextFields = W3CTraceContextPropagator.getInstance().fields();

        return Stream.of(datadogFields, b3Fields, traceContextFields)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }



    /**
     * Takes the given key-value pairs and the span context.
     *
     * @param spanToPropagate the span context to propagate, null if none shall be propagated
     *
     * @return the result propagation map
     */
    Map<String, String> buildPropagationHeaderMap(SpanContext spanToPropagate) {
        HashMap<String, String> result = new HashMap<>();
        if (spanToPropagate != null) {
            propagationFormat.inject(Context.current().with(Span.wrap(spanToPropagate)), result, MAP_INJECTOR);

            if (CustomIdGenerator.isUsing64Bit()) {
                String traceid = spanToPropagate.getTraceId();
                // do nothing in case trace ID is already 64bit
                if (traceid.length() > 16) {
                    for (Map.Entry<String, String> entry : result.entrySet()) {
                        // we only trim the value in case it contains only the trace id (which is not the case when using the w3c trace context format)
                        if (entry.getValue().equals(traceid)) {
                            entry.setValue(traceid.substring(16));
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Decodes a span context from the given header to value map into the given target context.
     *
     * @param propagationMap the headers to decode
     *
     * @return the {@code SpanContext} if the data contained any trace correlation, {@code null} otherwise.
     */
    SpanContext readPropagatedSpanContextFromHeaderMap(Map<String, String> propagationMap) {
        boolean anyB3Header = B3Format.INSTANCE.anyB3Header(propagationMap);
        if (anyB3Header) {
            try {
                Map<String, String> b3Headers = B3Format.INSTANCE.getB3Headers(propagationMap);
                return extractPropagatedSpanContext(B3Propagator.injectingMultiHeaders(), b3Headers);
            } catch (Exception ex) {
                String headerString = B3Format.INSTANCE.getB3HeadersAsString(propagationMap);
                log.error("Error reading trace correlation data from B3 headers: {}", headerString, ex);
            }
        }

        boolean anyTraceContextHeader = W3CTraceContextPropagator.getInstance()
                .fields()
                .stream()
                .anyMatch(propagationMap::containsKey);
        if (anyTraceContextHeader) {
            try {
                return extractPropagatedSpanContext(W3CTraceContextPropagator.getInstance(), propagationMap);
            } catch (Exception ex) {
                log.error("Error reading trace correlation data from the trace context headers", ex);
            }
        }

        boolean anyDatadogHeader = DatadogFormat.INSTANCE.fields().stream().anyMatch(propagationMap::containsKey);
        if (anyDatadogHeader) {
            try {
                return extractPropagatedSpanContext(DatadogFormat.INSTANCE, propagationMap);
            } catch (Exception ex) {
                log.error("Error reading trace correlation data from the Datadog headers", ex);
            }
        }

        return null;
    }

    /**
     * Extracts the {@link SpanContext} from the given {@code propagator}
     *
     * @param propagator
     * @param carrier holds the propagation fields
     *
     * @return the extracted span context
     */

    private SpanContext extractPropagatedSpanContext(TextMapPropagator propagator, Map<String, String> carrier) {
        return Span.fromContext(propagator.extract(Context.current(), carrier, MAP_EXTRACTOR)).getSpanContext();
    }

    /**
     * Sets the currently used propagation format to the specified one.
     *
     * @param format the format to use
     */
    void setPropagationFormat(PropagationFormat format) {
        switch (format) {
            case B3:
                log.info("Using B3 format for context propagation");
                propagationFormat = B3Propagator.injectingMultiHeaders();
                break;
            case TRACE_CONTEXT:
                log.info("Using TraceContext format for context propagation");
                propagationFormat = W3CTraceContextPropagator.getInstance();
                break;
            case DATADOG:
                log.info("Using Datadog format for context propagation");
                propagationFormat = DatadogFormat.INSTANCE;
                break;
            default:
                log.warn("The specified propagation format {} is not supported. Falling back to TraceContext format", format);
                propagationFormat = W3CTraceContextPropagator.getInstance();
        }
    }
}
