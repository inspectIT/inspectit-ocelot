package rocks.inspectit.oce.core.config.model.config;

import lombok.Data;

/**
 * If path is not null and enabled is true a {@link rocks.inspectit.oce.core.config.filebased.DirectoryPropertySource}
 * will be created for the given path. This configuration has the highest priority, meaning that it will be loaded first
 * and can configure other configuration sources.
 */
@Data
public class FileBasedConfigSettings {
    /**
     * The path to the directory containing the .yml or .properties files.
     * Can be null or empty, in which case no file based configuration is used.
     */
    String path;

    /**
     * Can be used to disable the file based config while the path is still specified.
     */
    boolean enabled;
}
