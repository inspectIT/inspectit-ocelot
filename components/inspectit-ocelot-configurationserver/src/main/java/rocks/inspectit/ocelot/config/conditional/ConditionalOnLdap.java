package rocks.inspectit.ocelot.config.conditional;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * Condition which is based on {@link OnLdapCondition}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnLdapCondition.class)
public @interface ConditionalOnLdap {

}
