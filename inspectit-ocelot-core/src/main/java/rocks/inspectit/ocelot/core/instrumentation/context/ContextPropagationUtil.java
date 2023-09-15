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
import rocks.inspectit.ocelot.core.instrumentation.context.propagation.BrowserPropagationUtil;
import rocks.inspectit.ocelot.core.instrumentation.context.propagation.DatadogFormat;
import rocks.inspectit.ocelot.core.opentelemetry.trace.CustomIdGenerator;

import javax.annotation.Nullable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements the logic for generating and reading the http headers related to context propagation.
 * Currently the propagation happens only via the Correlation-Context headers:
 * https://github.com/w3c/correlation-context/blob/master/correlation_context/HTTP_HEADER_FORMAT.md
 * When tracing is added, additional header formats, such as B3 will be used.
 */
@Slf4j
public class ContextPropagationUtil {

    /**
     * Maps each serializable type to its identifier.
     * If a non-string type is serialized, this d is used in the Correlation-Context Header, e.g.:
     * Correlation-Context: pi=3.14;type=d
     * (d is the identifier for "Double")
     */
    private static final Map<Class<?>, Character> TYPE_TO_ID_MAP = new HashMap<>();

    private static final Map<Character, Function<String, Object>> TYPE_ID_TO_PARSER_MAP = new HashMap<>();

    private static final String ENCODING_CHARSET = java.nio.charset.StandardCharsets.UTF_8.toString();

    public static final String CORRELATION_CONTEXT_HEADER = "Correlation-Context";

    /**
     * Session-ID-key to allow browser propagation
     */
    private static String SESSION_ID_HEADER = BrowserPropagationUtil.getSessionIdHeader();

    private static final String B3_HEADER_PREFIX = "X-B3-";

    private static final Pattern COMMA_WITH_WHITESPACES = Pattern.compile(" *, *");

    private static final Pattern SEMICOLON_WITH_WHITESPACES = Pattern.compile(" *; *");

    private static final Pattern EQUALS_WITH_WHITESPACES = Pattern.compile(" *= *");

    private static final Set<String> PROPAGATION_FIELDS = new HashSet<>();

    /**
     * The currently used propagation format. Defaults to {@link B3Propagator#injectingMultiHeaders()} ()}
     */
    private static TextMapPropagator propagationFormat = B3Propagator.injectingMultiHeaders();

    public static final TextMapSetter<Map<String, String>> MAP_INJECTOR = new TextMapSetter<Map<String, String>>() {
        @Override
        public void set(@Nullable Map<String, String> carrier, String key, String value) {
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

    static {
        PROPAGATION_FIELDS.add(CORRELATION_CONTEXT_HEADER);
        PROPAGATION_FIELDS.add(SESSION_ID_HEADER);
        PROPAGATION_FIELDS.addAll(B3Propagator.injectingSingleHeader().fields());
        PROPAGATION_FIELDS.addAll(B3Propagator.injectingMultiHeaders().fields());
        PROPAGATION_FIELDS.addAll(W3CTraceContextPropagator.getInstance().fields());
        PROPAGATION_FIELDS.addAll(DatadogFormat.INSTANCE.fields());
    }

    static {
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
     * Takes the given key-value pairs and encodes them into the Correlation-Context header.
     *
     * @param dataToPropagate the key-value pairs to propagate.
     *
     * @return the result propagation map
     */
    public static Map<String, String> buildPropagationHeaderMap(Stream<Map.Entry<String, Object>> dataToPropagate) {
        return buildPropagationHeaderMap(dataToPropagate, null);
    }

    /**
     * Takes the given key-value pairs and the span context and encodes them into the Correlation-Context header.
     *
     * @param dataToPropagate the key-value pairs to propagate.
     * @param spanToPropagate the span context to propagate, null if none shall be propagated
     *
     * @return the result propagation map
     */
    public static Map<String, String> buildPropagationHeaderMap(Stream<Map.Entry<String, Object>> dataToPropagate, SpanContext spanToPropagate) {
        String contextCorrelationData = buildCorrelationContextHeader(dataToPropagate);
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
        if (contextCorrelationData.length() > 0) {
            result.put(CORRELATION_CONTEXT_HEADER, contextCorrelationData);
        }

        return result;
    }

    private static String buildCorrelationContextHeader(Stream<Map.Entry<String, Object>> dataToPropagate) {
        StringBuilder contextCorrelationData = new StringBuilder();
        dataToPropagate.forEach(e -> {
            try {
                Object value = e.getValue();
                Character typeId = TYPE_TO_ID_MAP.get(value.getClass());
                if (value instanceof String || typeId != null) {
                    String key = e.getKey();
                    String encodedValue = URLEncoder.encode(value.toString(), ENCODING_CHARSET);
                    String encodedKey = URLEncoder.encode(key, ENCODING_CHARSET);
                    if (contextCorrelationData.length() > 0) {
                        contextCorrelationData.append(',');
                    }
                    contextCorrelationData.append(encodedKey).append('=').append(encodedValue);
                    if (typeId != null) {
                        contextCorrelationData.append(";type=").append(typeId);
                    }
                }
            } catch (Throwable t) {
                log.error("Error encoding correlation context header", e);
            }
        });
        return contextCorrelationData.toString();
    }

    /**
     * Returns all header names which can potentially be output by {@link #buildPropagationHeaderMap(Stream, SpanContext)}.
     *
     * @return the set of header names
     */
    public static Set<String> getPropagationHeaderNames() {
        return PROPAGATION_FIELDS;
    }

    /**
     * Decodes the given header to value map into the given target context.
     *
     * @param propagationMap the headers to decode
     * @param target         the context in which the decoded data key-value pairs will be stored.
     */
    public static void readPropagatedDataFromHeaderMap(Map<String, String> propagationMap, InspectitContextImpl target) {
        if (propagationMap.containsKey(CORRELATION_CONTEXT_HEADER)) {
            readCorrelationContext(propagationMap.get(CORRELATION_CONTEXT_HEADER), target);
        }
    }

    /**
     * Decodes a span context from the given header to value map into the given target context.
     *
     * @param propagationMap the headers to decode
     *
     * @return the {@code SpanContext} if the data contained any trace correlation, {@code null} otherwise.
     */
    public static SpanContext readPropagatedSpanContextFromHeaderMap(Map<String, String> propagationMap) {
        boolean anyB3Header = B3Propagator.injectingMultiHeaders()
                .fields()
                .stream()
                .anyMatch(s -> propagationMap.containsKey(s) && s.startsWith(B3_HEADER_PREFIX));
        if (anyB3Header) {
            try {
                return extractPropagatedSpanContext(B3Propagator.injectingMultiHeaders(), propagationMap);
            } catch (Throwable t) {
                String headerString = getB3HeadersAsString(propagationMap);
                log.error("Error reading trace correlation data from B3 headers: {}", headerString, t);
            }
        }

        boolean anyTraceContextHeader = W3CTraceContextPropagator.getInstance()
                .fields()
                .stream()
                .anyMatch(propagationMap::containsKey);
        if (anyTraceContextHeader) {
            try {
                return extractPropagatedSpanContext(W3CTraceContextPropagator.getInstance(), propagationMap);
            } catch (Throwable t) {
                log.error("Error reading trace correlation data from the trace context headers.", t);
            }
        }

        boolean anyDatadogHeader = DatadogFormat.INSTANCE.fields().stream().anyMatch(propagationMap::containsKey);
        if (anyDatadogHeader) {
            try {
                return extractPropagatedSpanContext(DatadogFormat.INSTANCE, propagationMap);
            } catch (Throwable t) {
                log.error("Error reading trace correlation data from the Datadog headers.", t);
            }
        }

        return null;
    }

    /**
     * Reads the session-id from the map with the current session-id-key
     *
     * @param propagationMap the headers to decode
     * @return session-id if existing, otherwise null
     */
    public static String readPropagatedSessionIdFromHeaderMap(Map<String,String> propagationMap) {
        return propagationMap.get(ContextPropagationUtil.SESSION_ID_HEADER);
    }

    /**
     * Extracts the {@link SpanContext} from the given {@code propagator}
     *
     * @param propagator
     * @param carrier    holds the propagation fields
     *
     * @return
     */

    private static SpanContext extractPropagatedSpanContext(TextMapPropagator propagator, Map<String, String> carrier) {
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
    static String getB3HeadersAsString(Map<String, String> headers) {
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
     * Parses the value of the Correlation-Context header, storing the propagated data values into the target context.
     *
     * @param correlationContext the value of the Correlation-Context header
     * @param target             the target context in which the data will be stored
     */
    private static void readCorrelationContext(String correlationContext, InspectitContextImpl target) {
        correlationContext = correlationContext.trim();
        for (String keyValuePair : COMMA_WITH_WHITESPACES.split(correlationContext)) {
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
            } catch (Throwable t) {
                log.error("Error decoding Correlation-Context header", t);
            }
        }
    }

    /**
     * Scans the given properties for a type=... definition.
     * If a correct definition is found, the given string value is parsed ot the given type and returned.
     * Otherwise the string value is returned unchanged.
     *
     * @param stringValue the value to parse
     * @param properties  the collection of property definition in the format "propertyname=value"
     *
     * @return the parsed value
     */
    private static Object parseTyped(String stringValue, Collection<String> properties) {
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
    public static void setPropagationFormat(PropagationFormat format) {
        switch (format) {
            case B3:
                log.info("Using B3 format for context propagation.");
                propagationFormat = B3Propagator.injectingMultiHeaders();
                break;
            case TRACE_CONTEXT:
                log.info("Using TraceContext format for context propagation.");
                propagationFormat = W3CTraceContextPropagator.getInstance();
                break;
            case DATADOG:
                log.info("Using Datadog format for context propagation.");
                propagationFormat = DatadogFormat.INSTANCE;
                break;
            default:
                log.warn("The specified propagation format {} is not supported. Falling back to B3 format.", format);
                propagationFormat = B3Propagator.injectingMultiHeaders();
        }
    }

    /**
     * Updates the current session-id-key used for browser propagation
     * @param sessionIdHeader new session-id-key
     */
    public static void setSessionIdHeader(String sessionIdHeader) {
        PROPAGATION_FIELDS.remove(SESSION_ID_HEADER);
        SESSION_ID_HEADER = sessionIdHeader;
        PROPAGATION_FIELDS.add(SESSION_ID_HEADER);
    }
}
