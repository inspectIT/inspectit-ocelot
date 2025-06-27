package rocks.inspectit.ocelot.core.instrumentation.context;

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
import rocks.inspectit.ocelot.core.instrumentation.context.propagation.PropagationFormatManager;
import rocks.inspectit.ocelot.core.instrumentation.context.session.SessionIdManager;
import rocks.inspectit.ocelot.core.instrumentation.context.propagation.DatadogFormat;
import rocks.inspectit.ocelot.core.opentelemetry.trace.CustomIdGenerator;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Singleton, which implements the logic for generating and reading the http headers related to context propagation.
 * Currently, the propagation happens only via the Baggage headers:
 * <a href="https://github.com/w3c/baggage/blob/main/baggage/HTTP_HEADER_FORMAT.md">Check out the W3C documentation</a>
 * <br>
 * When tracing is added, additional header formats, such as B3 will be used.
 */
@Slf4j
public class ContextPropagation {

    private static ContextPropagation instance;

    /**
     * Maps each serializable type to its identifier.
     * If a non-string type is serialized, this d is used in the Baggage Header, e.g.:
     * Baggage: pi=3.14;type=d
     * (d is the identifier for "Double")
     */
    private final Map<Class<?>, Character> TYPE_TO_ID_MAP = new HashMap<>();

    private final Map<Character, Function<String, Object>> TYPE_ID_TO_PARSER_MAP = new HashMap<>();

    private final String ENCODING_CHARSET = java.nio.charset.StandardCharsets.UTF_8.toString();

    /**
     * We use this header to propagate data between different services via HTTP
     */
    public final String BAGGAGE_HEADER = "Baggage";

    /**
     * We use this header to allow JavaScript in frontends to also read the {@code Baggage} header.
     * Normally, browser security prevents JavaScript to read such "custom" HTTP headers in cross-origin requests.
     * JVMs do not require this additional header to read {@code Baggage}.
     */
    public final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    private final String B3_HEADER_PREFIX = "X-B3-";

    private final Pattern COMMA_WITH_WHITESPACES = Pattern.compile(" *, *");

    private final Pattern SEMICOLON_WITH_WHITESPACES = Pattern.compile(" *; *");

    private final Pattern EQUALS_WITH_WHITESPACES = Pattern.compile(" *= *");

    private final Set<String> PROPAGATION_FIELDS = new HashSet<>();

    /**
     * The currently used propagation format. Defaults to {@link W3CTraceContextPropagator}.
     * Will be set via {@link PropagationFormatManager}
     */
    private TextMapPropagator propagationFormat = W3CTraceContextPropagator.getInstance();

    /**
     * HTTP header to read session ids. Defaults to {@code Session-Id}.
     * Will be set via {@link SessionIdManager}
     */
    private String sessionIdHeader = "Session-Id";

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

    // Private constructor for singleton
    private ContextPropagation() {
        addPropagationFields();
        addTypeParser();
    }

    private void addPropagationFields() {
        // We could try to use the W3CBaggagePropagator for baggage like the W3CTraceContextPropagator for traces
        PROPAGATION_FIELDS.add(BAGGAGE_HEADER);
        PROPAGATION_FIELDS.addAll(B3Propagator.injectingSingleHeader().fields());
        PROPAGATION_FIELDS.addAll(B3Propagator.injectingMultiHeaders().fields());
        PROPAGATION_FIELDS.addAll(W3CTraceContextPropagator.getInstance().fields());
        PROPAGATION_FIELDS.addAll(DatadogFormat.INSTANCE.fields());
    }

    private void addTypeParser() {
        TYPE_TO_ID_MAP.put(Byte.class, 'a'); //use a because b is already taken for boolean
        TYPE_ID_TO_PARSER_MAP.put('a', Byte::parseByte);
        TYPE_TO_ID_MAP.put(Short.class, 's');
        TYPE_ID_TO_PARSER_MAP.put('s', Short::parseShort);
        TYPE_TO_ID_MAP.put(Integer.class, 'i');
        TYPE_ID_TO_PARSER_MAP.put('i', Integer::parseInt);
        TYPE_TO_ID_MAP.put(Long.class, 'l');
        TYPE_ID_TO_PARSER_MAP.put('l', Long::parseLong);
        TYPE_TO_ID_MAP.put(Float.class, 'f');
        TYPE_ID_TO_PARSER_MAP.put('f', Float::parseFloat);
        TYPE_TO_ID_MAP.put(Double.class, 'd');
        TYPE_ID_TO_PARSER_MAP.put('d', Double::parseDouble);
        TYPE_TO_ID_MAP.put(Character.class, 'c');
        TYPE_ID_TO_PARSER_MAP.put('c', s -> s.charAt(0));
        TYPE_TO_ID_MAP.put(Boolean.class, 'b');
        TYPE_ID_TO_PARSER_MAP.put('b', Boolean::parseBoolean);
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
        String baggageHeader = buildBaggageHeader(dataToPropagate);
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
        if (!baggageHeader.isEmpty()) {
            result.put(BAGGAGE_HEADER, baggageHeader);
            // Make sure frontends can also read the baggage header
            result.put(ACCESS_CONTROL_EXPOSE_HEADERS, BAGGAGE_HEADER);
        }

        return result;
    }

    private String buildBaggageHeader(Map<String, Object> dataToPropagate) {
        StringBuilder baggageData = new StringBuilder();
        dataToPropagate.forEach((key,value) -> {
            try {
                Character typeId = TYPE_TO_ID_MAP.get(value.getClass());
                if (value instanceof String || typeId != null) {
                    String encodedValue = URLEncoder.encode(value.toString(), ENCODING_CHARSET);
                    String encodedKey = URLEncoder.encode(key, ENCODING_CHARSET);
                    if (baggageData.length() > 0) {
                        baggageData.append(',');
                    }
                    baggageData.append(encodedKey).append('=').append(encodedValue);
                    if (typeId != null) {
                        baggageData.append(";type=").append(typeId);
                    }
                }
            } catch (Exception ex) {
                log.error("Error encoding baggage header", ex);
            }
        });
        return baggageData.toString();
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
        if (propagationMap.containsKey(BAGGAGE_HEADER))
            readBaggage(propagationMap.get(BAGGAGE_HEADER), target);
    }

    /**
     * Decodes a span context from the given header to value map into the given target context.
     *
     * @param propagationMap the headers to decode
     *
     * @return the {@code SpanContext} if the data contained any trace correlation, {@code null} otherwise.
     */
    public SpanContext readPropagatedSpanContextFromHeaderMap(Map<String, String> propagationMap) {
        boolean anyB3Header = B3Propagator.injectingMultiHeaders()
                .fields()
                .stream()
                .anyMatch(s -> propagationMap.containsKey(s) && s.startsWith(B3_HEADER_PREFIX));
        if (anyB3Header) {
            try {
                return extractPropagatedSpanContext(B3Propagator.injectingMultiHeaders(), propagationMap);
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
                    .filter(entry -> entry.getKey().startsWith(B3_HEADER_PREFIX))
                    .map(entry -> "\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"")
                    .collect(Collectors.joining(", "));

            builder.append(headerString);
        }

        builder.append("]");
        return builder.toString();
    }

    /**
     * Parses the value of the Baggage header, storing the propagated data values into the target context.
     *
     * @param baggage the value of the Baggage header
     * @param target  the target context in which the data will be stored
     */
    private void readBaggage(String baggage, InspectitContextImpl target) {
        baggage = baggage.trim();
        for (String keyValuePair : COMMA_WITH_WHITESPACES.split(baggage)) {
            try {
                //split into assignment and attributes
                String[] pairAndProperties = SEMICOLON_WITH_WHITESPACES.split(keyValuePair);
                String[] keyAndValue = EQUALS_WITH_WHITESPACES.split(pairAndProperties[0]);
                if (keyAndValue.length != 2) {
                    continue;
                }
                String key = URLDecoder.decode(keyAndValue[0], ENCODING_CHARSET);
                String stringValue = URLDecoder.decode(keyAndValue[1], ENCODING_CHARSET);
                List<String> properties = Arrays.asList(pairAndProperties).subList(1, pairAndProperties.length);
                Object resultValue = parseTyped(stringValue, properties);
                target.setData(key, resultValue);
            } catch (Exception ex) {
                log.error("Error decoding Baggage header", ex);
            }
        }
    }

    /**
     * Scans the given properties for a type=... definition.
     * If a correct definition is found, the given string value is parsed ot the given type and returned.
     * Otherwise, the string value is returned unchanged.
     *
     * @param stringValue the value to parse
     * @param properties  the collection of property definition in the format "propertyname=value"
     *
     * @return the parsed value
     */
    private Object parseTyped(String stringValue, Collection<String> properties) {
        for (String property : properties) {
            String[] propertyNameAndValue = EQUALS_WITH_WHITESPACES.split(property);
            if (propertyNameAndValue.length == 2) {
                String propertyName = propertyNameAndValue[0];
                String propertyValue = propertyNameAndValue[1];
                if (propertyName.equals("type") && propertyValue.length() == 1) {
                    Function<String, Object> parser = TYPE_ID_TO_PARSER_MAP.get(propertyValue.charAt(0));
                    if (parser != null) {
                        return parser.apply(stringValue);
                    }
                }
            }
        }
        return stringValue;
    }

    /**
     * Sets the currently used propagation format to the specified one.
     *
     * @param format the format to use
     */
    public void setPropagationFormat(PropagationFormat format) {
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
                log.warn("The specified propagation format {} is not supported. Falling back to B3 format", format);
                propagationFormat = B3Propagator.injectingMultiHeaders();
        }
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
