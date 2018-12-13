package rocks.inspectit.oce.core.config.model.config;

import lombok.Data;

/**
 * Defines the settings for all configuration sources
 */
@Data
public class ConfigSettings {

    /**
     * Settings for file-based configuration input.
     */
    FileBasedConfigSettings fileBased;

}
