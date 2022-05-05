package rocks.inspectit.ocelot.config.model.exporters;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

/**
 * Validator for {@link TransportProtocolSubset}
 */

public class TransportProtocolSubsetValidator implements ConstraintValidator<TransportProtocolSubset, Enum> {

    private TransportProtocol[] subset;

    @Override
    public void initialize(TransportProtocolSubset constraintAnnotation) {
        subset = constraintAnnotation.anyOf();
    }

    @Override
    public boolean isValid(Enum value, ConstraintValidatorContext context) {
        return Arrays.asList(subset).contains(value);
    }
}

