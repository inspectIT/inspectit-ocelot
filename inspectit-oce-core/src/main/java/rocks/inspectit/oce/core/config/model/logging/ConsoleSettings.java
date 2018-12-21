package rocks.inspectit.oce.core.config.model.logging;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConsoleSettings {

    private boolean enabled;
    private String pattern;

}
