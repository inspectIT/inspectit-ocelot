package parsing;

import org.apache.commons.text.StringSubstitutor;

import java.util.Map;

public class StringSubstitutorNestedMap extends StringSubstitutor {

    public StringSubstitutorNestedMap(Map<String, Object> valueMap){
        this.setVariableResolver(new StringLookupNestedMap(valueMap));
    }

}
