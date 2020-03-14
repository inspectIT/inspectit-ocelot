package rocks.inspectit.ocelot.core.privacy.obfuscation;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;

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
        AttributeValue attributeValue = null;
        if(value instanceof Double){
            attributeValue = AttributeValue.doubleAttributeValue((Double)value);
        } else if (value instanceof Number){
            attributeValue = AttributeValue.longAttributeValue(((Number)value).longValue());
        } else if (value instanceof Boolean){
            attributeValue = AttributeValue.booleanAttributeValue((Boolean)value);
        } else {
            attributeValue = AttributeValue.stringAttributeValue(value.toString());
        }

        span.putAttribute(key, attributeValue);
    }

}
