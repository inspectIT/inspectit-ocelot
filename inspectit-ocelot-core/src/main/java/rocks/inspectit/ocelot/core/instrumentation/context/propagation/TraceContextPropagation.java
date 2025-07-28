package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
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

    private static final String B3_HEADER_PREFIX = "X-B3-";

    /** All b3 single-header fields we support */
    private static final List<String> b3SingleHeaderFields  = getB3Fields(B3Propagator.injectingSingleHeader());

    /** All b3 multi-header fields we support */
    private static final List<String> b3MultiHeaderFields = getB3Fields(B3Propagator.injectingMultiHeaders());

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
        Collection<String> traceContextFields = W3CTraceContextPropagator.getInstance().fields();

        return Stream.of(datadogFields, traceContextFields, b3SingleHeaderFields, b3MultiHeaderFields)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * @return all b3 header fields, also supporting lowercase
     */
    private static List<String> getB3Fields(B3Propagator propagator) {
        return propagator.fields().stream()
                .flatMap(field -> Stream.of(field, field.toLowerCase()))
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
        boolean anyB3Header = b3MultiHeaderFields
                .stream()
                .anyMatch(s -> propagationMap.containsKey(s) && startsWithB3Prefix(s));
        if (anyB3Header) {
            try {
                return extractPropagatedSpanContext(B3Propagator.injectingMultiHeaders(), getB3Headers(propagationMap));
            } catch (Exception ex) {
                String headerString = getB3HeadersAsString(propagationMap);
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
     * Returns a string representation of all B3 headers (headers which key is starting with {@link #B3_HEADER_PREFIX})
     * in the given map.
     *
     * @param headers the map containing the headers
     *
     * @return string representation of the headers (["key": "value"]).
     */
    @VisibleForTesting
    String getB3HeadersAsString(Map<String, String> headers) {
        StringBuilder builder = new StringBuilder("[");

        if (!CollectionUtils.isEmpty(headers)) {
            String headerString = headers.entrySet()
                    .stream()
                    .filter(entry -> startsWithB3Prefix(entry.getKey()))
                    .map(entry -> "\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"")
                    .collect(Collectors.joining(", "));

            builder.append(headerString);
        }

        builder.append("]");
        return builder.toString();
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

    /**
     * Helper method to support lowercase b3-headers
     *
     * @return true, if the header starts with the b3 prefix
     */
    private boolean startsWithB3Prefix(String header) {
        return header.startsWith(B3_HEADER_PREFIX) || header.startsWith(B3_HEADER_PREFIX.toLowerCase());
    }

    /**
     * Filter for the b3-headers. If the b3-header names are lowercase, they will be transformed into capitalized
     * header names, because {@link B3Propagator} only support capitalized headers. <br>
     * For example: {@code x-b3-traceid} -> {@code X-B3-TraceId} <br>
     * We expect these headers: {@link io.opentelemetry.extension.trace.propagation.B3PropagatorInjectorMultipleHeaders#FIELDS}
     *
     * @param headers the headers
     *
     * @return the b3-headers with capitalized names
     */
    public Map<String, String> getB3Headers(Map<String, String> headers) {
       Map<String, String> b3Headers = new HashMap<>();
       headers.forEach((header, value) -> {
           if(header.startsWith(B3_HEADER_PREFIX))
               b3Headers.put(header, value); // already capitalized
           else if(header.startsWith(B3_HEADER_PREFIX.toLowerCase())) {
               // There should be only these
               switch (header) {
                   case "x-b3-traceid":
                       b3Headers.put("X-B3-TraceId", value);
                       break;
                   case "x-b3-spanid":
                       b3Headers.put("X-B3-SpanId", value);
                       break;
                   case "x-b3-sampled":
                       b3Headers.put("X-B3-Sampled", value);
                       break;
                   default: log.debug("Unknown B3-header: {}", header);
                }
            }
        });
        return b3Headers;
    }
}
