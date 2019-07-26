package rocks.inspectit.ocelot.config.model.plugins;

import lombok.Data;

/**
 * General plugin settings.
 * Plugin specific settings are also placed under inspectit.plugins.{pluginName}.
 * However, these settings are not parsed into the {@link rocks.inspectit.ocelot.config.model.InspectitConfig},
 * instead they are parsed individually for each plugin.
 */
@Data
public class PluginSettings {

    /**
     * The path of the directory to scan for plugins.
     */
    String path;
}
