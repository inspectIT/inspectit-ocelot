package rocks.inspectit.ocelot.config.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RemoteRepositorySettingsValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteRepositorySettingsConstraint {

    String message() default "Invalid remote configuration settings.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
