package rocks.inspectit.ocelot.config.model.exporters;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validate that an {@link TransportProtocol} is in the supported array of {@link #anyOf()}.
 * Will fail if a property is not in {@link #anyOf()}.
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = TransportProtocolSubsetValidator.class)
public @interface TransportProtocolSubset {

    TransportProtocol[] anyOf();

    String message() default "Wrong 'protocol' is specified. Supported values are {anyOf}.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

