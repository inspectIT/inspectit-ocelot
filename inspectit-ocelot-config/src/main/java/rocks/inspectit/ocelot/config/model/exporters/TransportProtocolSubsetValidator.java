package rocks.inspectit.ocelot.config.utils;

import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

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

