package rocks.inspectit.ocelot.config.model.plugins;

import lombok.Data;

@Data
public class PluginSettings {

    /**
     * The path of the directory to scan for plugins.
     */
    String path;
}
