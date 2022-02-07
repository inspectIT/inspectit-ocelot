package rocks.inspectit.ocelot.config.model.selfmonitoring;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;
import java.util.logging.Level;

@Data
@NoArgsConstructor
public class LogsPreloadingSettings {

    /**
     * Whether commands are enabled or not.
     */
    private boolean enabled = false;

    /**
     * The URL for fetching agent commands.
     */
    private URL url;

    /**
     * The used log level and higher
     */
    private Level logLevel = Level.WARNING;

    /**
     * Buffer size
     */
    private int bufferSize;
}
