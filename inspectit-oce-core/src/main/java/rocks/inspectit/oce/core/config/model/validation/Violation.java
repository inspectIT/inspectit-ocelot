package rocks.inspectit.oce.core.config.model.validation;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
class Violation {
    private String message;
    @Singular
    private Map<String, String> parameters;

    @Singular
    private List<String> beanNodes;

}
