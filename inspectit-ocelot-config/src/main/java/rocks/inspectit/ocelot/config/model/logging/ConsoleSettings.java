package rocks.inspectit.ocelot.config.model.logging;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConsoleSettings {

    /**
     * If console-based logging is enabled.
     */
    private boolean enabled;

    /**
     * Custom pattern to use.
     */
    private String pattern;

}
