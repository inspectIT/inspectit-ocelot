package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import io.opentelemetry.extension.trace.propagation.B3Propagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Trace-context propagation with the B3 header format.
 */
@Slf4j
public class B3Format {

    public static final B3Format INSTANCE = new B3Format();

    private static final String B3_HEADER_PREFIX = "X-B3-";

    /** All b3 single-header fields we support */
    private static final List<String> b3SingleHeaderFields  = getB3Fields(B3Propagator.injectingSingleHeader());

    /** All b3 multi-header fields we support */
    private static final List<String> b3MultiHeaderFields = getB3Fields(B3Propagator.injectingMultiHeaders());

    private B3Format() {}

    /**
     * @return the fields that will be used to read b3 trace-context propagation data
     */
    public List<String> fields() {
        return Stream.concat(b3SingleHeaderFields.stream(), b3MultiHeaderFields.stream()).collect(Collectors.toList());
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
     * @param propagationMap the map with propagation data
     * @return true, if the provided map contains b3-headers
     */
    boolean anyB3Header(Map<String, String> propagationMap) {
        return b3MultiHeaderFields.stream()
                .anyMatch(s -> propagationMap.containsKey(s) && startsWithB3Prefix(s));
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
    Map<String, String> getB3Headers(Map<String, String> headers) {
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

    /**
     * Returns a string representation of all B3 headers (headers which key is starting with {@link #B3_HEADER_PREFIX})
     * in the given map.
     *
     * @param headers the map containing the headers
     *
     * @return string representation of the headers (["key": "value"]).
     */
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
     * Helper method to support lowercase b3-headers
     *
     * @return true, if the header starts with the b3 prefix
     */
    private boolean startsWithB3Prefix(String header) {
        return header.startsWith(B3_HEADER_PREFIX) || header.startsWith(B3_HEADER_PREFIX.toLowerCase());
    }
}
