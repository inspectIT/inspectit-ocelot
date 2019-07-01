package rocks.inspectit.ocelot.sdk;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OcelotPlugin {

    /**
     * @return The name of the plugin. The plugins configuration will be placed under inspectit.plugins.{name}.
     * Has to be lower kebab-case (e.g. my-plugin-name instead of MyPluginName).
     */
    String value();

    /**
     * @return The path to a YAML resource file within the jar containing default configurations for this plugin.
     * The defaults can be overridden by the user using any configuration source (JVM args, file-based, http, ..)
     * The name is resolved relative to the package of the plugin.
     * If this is empty, no default configuration properties will be loaded.
     */
    String defaultConfig() default "";
}
