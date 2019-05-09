package rocks.inspectit.ocelot.config.model.config;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines the settings for all configuration sources
 */
@Data
@NoArgsConstructor
public class ConfigSettings {

    /**
     * Settings for file-based configuration input.
     */
    private FileBasedConfigSettings fileBased;

}
