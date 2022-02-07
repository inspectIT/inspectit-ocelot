package rocks.inspectit.ocelot.config.model.selfmonitoring;

import ch.qos.logback.classic.Level;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;

@Data
@NoArgsConstructor
public class LogPreloadingSettings {

    /**
     * Whether commands are enabled or not.
     */
    private boolean enabled = false;

    /**
     * The URL for fetching agent commands.
     */
    // TODO: not needed! This is controlled by central agent command handling.
    private URL url;

    /**
     * The used log level and higher
     */
    // TODO: changed it to logback level for compatibility. Please remove this line when read.
    private Level logLevel = Level.WARN;

    /**
     * Buffer size
     */
    private int bufferSize;
}
