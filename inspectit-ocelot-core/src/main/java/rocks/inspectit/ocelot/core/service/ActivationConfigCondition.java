package rocks.inspectit.ocelot.core.service;

import org.springframework.context.annotation.Conditional;
import rocks.inspectit.ocelot.core.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.model.config.ConfigSettings;
import rocks.inspectit.ocelot.core.config.model.config.FileBasedConfigSettings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify a condition on {@link InspectitConfig} for a bean in Spring-Expression Language.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Conditional(ActivationConfigConditionMet.class)
public @interface ActivationConfigCondition {
    /**
     * The condition in Spring Expression Language to check if it evaluates to "true".
     * The condition is applied with the {@link InspectitConfig} as root,
     * so for example "config.fileBased.watch" refers to {@link FileBasedConfigSettings#isWatch()}
     * within {@link ConfigSettings}.
     *
     * @return
     */
    String value();
}
