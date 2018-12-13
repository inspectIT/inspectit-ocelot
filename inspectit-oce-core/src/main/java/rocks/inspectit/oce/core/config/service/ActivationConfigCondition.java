package rocks.inspectit.oce.core.config.service;

import rocks.inspectit.oce.core.config.model.config.FileBasedConfigSettings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify a condition for {@link ActivationConfigConditionMet} in Spring-Expression Language.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ActivationConfigCondition {
    /**
     * The condition in SPring Expression Language to check if it evaluates to "true".
     * The condition is applied with the {@link rocks.inspectit.oce.core.config.model.InspectitConfig} as root,
     * so for example "config.fileBased.watch" refers to {@link FileBasedConfigSettings#isWatch()}
     * within {@link rocks.inspectit.oce.core.config.model.config.ConfigSettings}.
     *
     * @return
     */
    String value();
}
