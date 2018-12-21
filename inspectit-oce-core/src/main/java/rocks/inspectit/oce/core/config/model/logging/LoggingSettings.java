package rocks.inspectit.oce.core.config.model.logging;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.FileSystemResource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class LoggingSettings {

    private FileSystemResource configFile;
    private boolean trace;
    private boolean debug;
    @NotNull
    @Valid
    private ConsoleSettings console;
    @NotNull
    @Valid
    private FileSettings file;

}
