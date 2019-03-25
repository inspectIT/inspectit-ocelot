package rocks.inspectit.ocelot.core.config.model.validation;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
/**
 * Class for defining a custom constraint violation with a parametrized message,
 * the parameter values and the bean path to the property.
 */
public class Violation {

    private String message;

    @Singular
    private Map<String, String> parameters;

    /**
     * The nodes defining the path to the bean, e.g. ["propA", "propB"]represent the path propA.propB
     */
    @Singular
    private List<String> beanNodes;

}
