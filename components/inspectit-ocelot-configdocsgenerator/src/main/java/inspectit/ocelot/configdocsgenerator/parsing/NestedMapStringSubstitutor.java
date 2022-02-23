package inspectit.ocelot.configdocsgenerator.parsing;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Version of {@link StringSubstitutor} with the possibility to use a nested Map to define values for the replaced variables.
 */
public class NestedMapStringSubstitutor extends StringSubstitutor implements StringLookup {

    private final Map<String, Object> valueMap;

    public NestedMapStringSubstitutor(Map<String, Object> valueMap) {
        this.valueMap = valueMap;
        setVariableResolver(this);
    }

    @Override
    public String lookup(String variableName) {

        List<String> keys = Arrays.asList(variableName.split("\\."));
        Object value = valueMap;
        StringBuilder old_key = new StringBuilder();

        for (String key : keys) {

            //needs to be casted each time because Java does not know that within the Map there are more Maps
            Object new_value = ((Map) value).get(old_key + key);

            //Some keys themselves contain dots again, which previously were used as split points, e.g. there is a key
            //concurrent.phase.time which points to one boolean value, so if no value is found with one key, it is
            // concatenated with the next one on the next round of the loop.
            if (new_value != null) {
                //If that is not the case, simply replace the old Map with the newly found one.
                value = new_value;
            } else {
                if (keys.get(keys.size() - 1).equals(key)) {
                    // if the corresponding value can not be found, return null which leads to the full variable being
                    // kept in the YAML
                    return null;
                } else {
                    old_key.append(key).append(".");
                }
            }
        }

        return value.toString();
    }
}