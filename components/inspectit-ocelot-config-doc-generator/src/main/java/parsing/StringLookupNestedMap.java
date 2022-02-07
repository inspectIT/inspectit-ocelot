package parsing;

import lombok.Data;
import org.apache.commons.text.lookup.StringLookup;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * StringLookup that uses a nested Map to find the value for a given variable.
 */
@Data
public class StringLookupNestedMap implements StringLookup {

    private final Map<String, Object> valueMap;

    @Override
    public String lookup(String variableName){

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
                if(keys.get(keys.size() - 1).equals(key)){
                    // if the corresponding value can not be found, return the full key.
                    // This is a workaround for, as of now, only environment variables, so it should be fine for the
                    // Documentation but would not be for any actually running agents.
                    return null;
                } else {
                    old_key.append(key).append(".");
                }
            }
        }

        return value.toString();
    }

}
