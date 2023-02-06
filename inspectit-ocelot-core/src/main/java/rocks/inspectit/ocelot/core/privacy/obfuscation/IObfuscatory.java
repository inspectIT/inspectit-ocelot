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
        if (value instanceof String) {
            attributeKey = AttributeKey.stringKey(key);
        } else if (value instanceof Double || value instanceof Float) {
            attributeKey = AttributeKey.doubleKey(key);
            if (value instanceof Float) {
                resultValue = Double.valueOf(((Float) value).floatValue());
                // alternatively, if we do not want the added precision
                // resultValue = Double.parseDouble(value.toString());
            }
        } else if (value instanceof Long) {
            attributeKey = AttributeKey.longKey(key);
        } else if (value instanceof Integer) {
            attributeKey = AttributeKey.longKey(key);
            resultValue = Long.valueOf(((Integer) value).longValue());
        } else if (value instanceof Number) {
            attributeKey = AttributeKey.doubleKey(key);
            resultValue = Double.valueOf(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            attributeKey = AttributeKey.booleanKey(key);
        } else {
            attributeKey = AttributeKey.stringKey(key);
            resultValue = value.toString();
        }

        span.setAttribute(attributeKey, resultValue);
    }

}
