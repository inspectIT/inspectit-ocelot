package rocks.inspectit.ocelot.config.model.selfmonitoring;

import ch.qos.logback.classic.Level;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

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
    @NotNull
    private Level logLevel = Level.WARN;

    /**
     * Buffer size
     */
    @Min(1)
    private int bufferSize;

}
