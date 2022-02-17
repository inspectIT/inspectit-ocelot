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
     * The used log level and higher
     */
    private Level logLevel = Level.WARN;

    /**
     * Buffer size
     */
    private int bufferSize;
}
