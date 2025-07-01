package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Currently, the propagation happens only via the Baggage headers:
 * <a href="https://github.com/w3c/baggage/blob/main/baggage/HTTP_HEADER_FORMAT.md">Check out the W3C documentation</a>
 */
@Slf4j
public class BaggagePropagation {

    /**
     * We use this header to propagate data between different services via HTTP
     */
    public static final String BAGGAGE_HEADER = "Baggage";

    /**
     * We use this header to allow JavaScript in frontends to also read the {@code Baggage} header.
     * Normally, browser security prevents JavaScript to read such "custom" HTTP headers in cross-origin requests.
     * JVMs do not require this additional header to read {@code Baggage}.
     */
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    /**
     * We allow maximum baggage of 4 KB. Most tools should not have a problem with this header size.
     * Also, we reject baggage greater than 4 KB for security reasons.
     */
    public static final int MAX_BAGGAGE_HEADER_SIZE = 4096;

    /**
     * Maps each serializable type to its identifier.
     * If a non-string type is serialized, this d is used in the Baggage Header, e.g.:
     * Baggage: pi=3.14;type=d
     * (d is the identifier for "Double")
     */
    private final Map<Class<?>, Character> TYPE_TO_ID_MAP = new HashMap<>();

    private final Map<Character, Function<String, Object>> TYPE_ID_TO_PARSER_MAP = new HashMap<>();

    private final String ENCODING_CHARSET = StandardCharsets.UTF_8.toString();

    private final Pattern COMMA_WITH_WHITESPACES = Pattern.compile(" *, *");

    private final Pattern SEMICOLON_WITH_WHITESPACES = Pattern.compile(" *; *");

    private final Pattern EQUALS_WITH_WHITESPACES = Pattern.compile(" *= *");

    BaggagePropagation() {
        addTypeParser();
    }

    /**
     * Decodes the given header to value map into the given target context.
     *
     * @param propagationMap the headers to decode
     * @param target         the context in which the decoded data key-value pairs will be stored.
     * @param propagation    the function to test, if a data key is configured for global propagation
     */
    void readPropagatedDataFromHeaderMap(Map<String, String> propagationMap, InspectitContextImpl target, Predicate<String> propagation) {
        if (propagationMap.containsKey(BAGGAGE_HEADER))
            readBaggage(propagationMap.get(BAGGAGE_HEADER), target, propagation);
    }

    /**
     * Parses the value of the Baggage header, storing the propagated data values into the target context.
     *
     * @param baggage the value of the Baggage header
     * @param target  the target context in which the data will be stored
     * @param propagation the function to test, if a data key is configured for global propagation
     */
    private void readBaggage(String baggage, InspectitContextImpl target, Predicate<String> propagation) {
        baggage = baggage.trim();

        if(baggage.length() > MAX_BAGGAGE_HEADER_SIZE) {
            log.debug("Incoming baggage header exceeds maximum header size and will not be read");
            return;
        }

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

                if(propagation.test(key))
                    target.setData(key, resultValue);
            } catch (Exception ex) {
                log.error("Error decoding Baggage header", ex);
            }
        }
    }

    /**
     * Takes the given key-value pairs and encodes them into the Baggage header.
     * For up-propagation, we will add a header to allow JavaScript to read the baggage in cross-origin requests.
     *
     * @param dataToPropagate the key-value pairs to propagate.
     * @param isUpPropagation flag for up-propagation
     *
     * @return the result propagation map
     */
    Map<String, String> buildBaggageHeaderMap(Map<String, Object> dataToPropagate, boolean isUpPropagation) {
        String baggageHeader = buildBaggageHeader(dataToPropagate);
        Map<String, String> result = new HashMap<>();

        if (!baggageHeader.isEmpty()) {
            result.put(BAGGAGE_HEADER, baggageHeader);

            if(isUpPropagation) result.put(ACCESS_CONTROL_EXPOSE_HEADERS, BAGGAGE_HEADER);
        }

        return result;
    }

    @VisibleForTesting
    String buildBaggageHeader(Map<String, Object> dataToPropagate) {
        StringBuilder baggageData = new StringBuilder();
        int headerSize = 0;

        for (Map.Entry<String, Object> entry : dataToPropagate.entrySet()) {
            try {
                Object value = entry.getValue();
                Character typeId = TYPE_TO_ID_MAP.get(value.getClass());
                if (value instanceof String || typeId != null) {
                    String encodedValue = URLEncoder.encode(value.toString(), ENCODING_CHARSET);
                    String encodedKey = URLEncoder.encode(entry.getKey(), ENCODING_CHARSET);
                    StringBuilder entryBuilder = new StringBuilder();

                    if (baggageData.length() > 0)
                        entryBuilder.append(',');

                    entryBuilder.append(encodedKey).append('=').append(encodedValue);

                    if (typeId != null)
                        entryBuilder.append(";type=").append(typeId);

                    String nextEntry = entryBuilder.toString();
                    int entrySize = calculateSize(nextEntry);

                    if(headerSize + entrySize > MAX_BAGGAGE_HEADER_SIZE) {
                        log.debug("Outgoing baggage header has exceeded maximum header size");
                        break;
                    }

                    headerSize += entrySize;
                    baggageData.append(nextEntry);
                }
            } catch (Exception ex) {
                log.error("Error encoding baggage header", ex);
            }
        }
        return baggageData.toString();
    }

    private int calculateSize(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
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
}
