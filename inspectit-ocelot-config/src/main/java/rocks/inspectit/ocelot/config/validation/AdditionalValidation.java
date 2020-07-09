package rocks.inspectit.ocelot.config.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be added to mark public methods which take only a {@link ViolationBuilder} as arguments.
 * When marked and if the containing class is annotated with {@link AdditionalValidations}, this methods will
 * get evalauted during the validation phase
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AdditionalValidation {

    /**
     * A template for the message of the {@link ViolationBuilder}
     *
     * @return the message template
     */
    String value() default "";
}
