package rocks.inspectit.ocelot.config.ui;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows to customize how "plain" properties are presented in the configuration UI.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface UISettings {

    /**
     * The name to use in the UI for the property.
     * Defaults to the camel-case name being split into words in Upper Case.
     * E.g. myPropertyValue gets the default "My Property Value".
     *
     * @return the name
     */
    String name() default "";

    /**
     * If true, the given property will not be editable via the UI.
     *
     * @return the exclude flag
     */
    boolean exclude() default false;
}
