package rocks.inspectit.oce.core.config.service;

import org.springframework.context.annotation.Conditional;
import rocks.inspectit.oce.core.config.model.config.FileBasedConfigSettings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify a condition on {@link rocks.inspectit.oce.core.config.model.InspectitConfig} for a bean in Spring-Expression Language.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Conditional(ActivationConfigConditionMet.class)
public @interface ActivationConfigCondition {
    /**
     * The condition in Spring Expression Language to check if it evaluates to "true".
     * The condition is applied with the {@link rocks.inspectit.oce.core.config.model.InspectitConfig} as root,
     * so for example "config.fileBased.watch" refers to {@link FileBasedConfigSettings#isWatch()}
     * within {@link rocks.inspectit.oce.core.config.model.config.ConfigSettings}.
     *
     * @return
     */
    String value();
}
