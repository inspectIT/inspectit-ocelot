package rocks.inspectit.ocelot.config.model.config;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

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

    /**
     * Settings for http property source.
     */
    @Valid
    private HttpConfigSettings http;
}
