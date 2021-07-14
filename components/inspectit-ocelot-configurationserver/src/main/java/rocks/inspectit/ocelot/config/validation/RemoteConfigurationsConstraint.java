package rocks.inspectit.ocelot.config.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RemoteConfigurationsValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteConfigurationsConstraint {

    String message() default "Invalid remote configuration settings.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
