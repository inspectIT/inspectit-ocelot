package rocks.inspectit.ocelot.core.config.model.logging;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConsoleSettings {

    /**
     * If file-based logging is enabled.
     */
    private boolean enabled;

    /**
     * Custom pattern to use.
     */
    private String pattern;

}
