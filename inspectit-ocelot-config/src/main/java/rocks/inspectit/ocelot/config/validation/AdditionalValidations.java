package rocks.inspectit.ocelot.config.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Can be applied on a class.
 * This causes that all methods annotated with an {@link AdditionalValidation}
 * which take a {@link ViolationBuilder} as argument get executed.
 * This allows a more flexible validation other than with {@link javax.validation.constraints.AssertTrue} or {@link javax.validation.constraints.AssertFalse}
 * because {@link ViolationBuilder} allows to customize the property path and the message.
 */
@Target({ElementType.TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = AdditionalValidationsValidator.class)
@Documented
public @interface AdditionalValidations {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
