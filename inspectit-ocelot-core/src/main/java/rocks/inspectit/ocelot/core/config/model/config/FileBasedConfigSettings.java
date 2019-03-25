package rocks.inspectit.ocelot.core.config.model.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import rocks.inspectit.ocelot.core.config.filebased.ConfigurationDirectoriesWatcher;
import rocks.inspectit.ocelot.core.config.filebased.DirectoryPropertySource;

import java.time.Duration;

/**
 * If path is not null and enabled is true a {@link DirectoryPropertySource}
 * will be created for the given path. This configuration has the highest priority, meaning that it will be loaded first
 * and can configure other configuration sources.
 */
@Data
@NoArgsConstructor
public class FileBasedConfigSettings {
    /**
     * The path to the directory containing the .yml or .properties files.
     * Can be null or empty, in which case no file based configuration is used.
     */
    private String path;

    /**
     * Can be used to disable the file based config while the path is still specified.
     */
    private boolean enabled;

    /**
     * If true, a {@link ConfigurationDirectoriesWatcher} will be started to reload the configuration from the directory on changes.
     */
    private boolean watch;

    /**
     * The frequency at which the target folder should be polled for changes if {@link #watch} is true.
     * If the frequency is set to zero, the java {@link java.nio.file.WatchService} is used instead of polling.
     */
    @NonNull
    private Duration frequency;
}
