package rocks.inspectit.ocelot.core.privacy.obfuscation;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;

public interface IObfuscatory {

    /**
     * Stores span attribute with optional obfuscation of the value if needed.
     * <p>
     * Default implementation performs no obfuscation.
     *
     * @param span  span to store attribute to
     * @param key   attribute key
     * @param value attribute value
     */
    default void putSpanAttribute(Span span, String key, Object value) {
        AttributeKey attributeKey;
        Object resultValue = value;

        // set the AttributeKey and, if applicable, the result value

        // String as String
        if (value instanceof String) {
            attributeKey = AttributeKey.stringKey(key);
        }
        // Double and Float as Double
        else if (value instanceof Double || value instanceof Float) {
            attributeKey = AttributeKey.doubleKey(key);
            if (value instanceof Float) {
                // as we do not want the added precision, we need to use the String representation instead of the floatValue()
                resultValue = Double.valueOf(value.toString());
            }
        }
        // Long, Integer and Short as Long
        else if (value instanceof Long || value instanceof Integer || value instanceof Short) {
            attributeKey = AttributeKey.longKey(key);
            resultValue = ((Number) value).longValue();
        }
        // treat all other Number as double
        else if (value instanceof Number) {
            attributeKey = AttributeKey.doubleKey(key);
            resultValue = ((Number) value).doubleValue();
        } else if (value instanceof Boolean) {
            attributeKey = AttributeKey.booleanKey(key);
        }
        // everything else (e.g., Exceptions) will be converted to String
        else {
            attributeKey = AttributeKey.stringKey(key);
            resultValue = value.toString();
        }

        span.setAttribute(attributeKey, resultValue);
    }

}
