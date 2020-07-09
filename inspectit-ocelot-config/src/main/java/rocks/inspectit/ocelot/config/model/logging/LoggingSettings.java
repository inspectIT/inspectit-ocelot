package rocks.inspectit.ocelot.config.model.logging;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.FileSystemResource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class LoggingSettings {

    /**
     * Custom configuration file specified by a user.
     */
    private FileSystemResource configFile;

    // below only properties that are related to default logback config

    /**
     * If inspectIT should log in TRACE level.
     */
    private boolean trace;

    /**
     * If inspectIT should log in TRACE level.
     */
    private boolean debug;

    /**
     * Settings for the console output.
     */
    @NotNull
    @Valid
    private ConsoleSettings console;

    /**
     * Settings for the file appender.
     */
    @NotNull
    @Valid
    private FileSettings file;

}
