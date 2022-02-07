package parsing;

import org.apache.commons.text.StringSubstitutor;

import java.util.Map;

/**
 * Version of StringSubstitutor with the possibility to use a nested Map to define values for the replaced variables.
 */
public class StringSubstitutorNestedMap extends StringSubstitutor {

    public StringSubstitutorNestedMap(Map<String, Object> valueMap){
        this.setVariableResolver(new StringLookupNestedMap(valueMap));
    }

}
