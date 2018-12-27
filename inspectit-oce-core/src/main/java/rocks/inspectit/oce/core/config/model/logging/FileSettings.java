package rocks.inspectit.oce.core.config.model.logging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@NoArgsConstructor
public class FileSettings {

    /**
     * If file-based logging is enabled.
     */
    private boolean enabled;

    /**
     * Custom pattern to use.
     */
    private String pattern;

    /**
     * Path to store the logs to.
     */
    private Path path;

    /**
     * If log pattern should include the service name property
     */
    private boolean includeServiceName;

}
