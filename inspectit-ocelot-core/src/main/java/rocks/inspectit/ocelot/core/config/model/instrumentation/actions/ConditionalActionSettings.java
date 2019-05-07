package rocks.inspectit.ocelot.core.config.model.instrumentation.actions;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class for all kinds of actions to which conditions can be added.
 */
@Data
@NoArgsConstructor
public class ConditionalActionSettings {
    /**
     * If not null, this field specifies a data-key.
     * In this case the action is only executed if the data assigned to this key is null.
     */
    private String onlyIfNull;
    /**
     * If not null, this field specifies a data-key.
     * In this case the action is only executed if the data assigned to this key is not null.
     */
    private String onlyIfNotNull;
    /**
     * If not null, this field specifies a data-key.
     * In this case the action is only executed if the data assigned to this key is a boolean with the value "true".
     */
    private String onlyIfTrue;
    /**
     * If not null, this field specifies a data-key.
     * In this case the action is only executed if the data assigned to this key is a boolean with the value "false".
     */
    private String onlyIfFalse;
}
