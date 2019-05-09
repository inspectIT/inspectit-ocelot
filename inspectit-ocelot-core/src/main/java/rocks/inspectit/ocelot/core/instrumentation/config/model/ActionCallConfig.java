package rocks.inspectit.ocelot.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Value;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;

/**
 * Combines a resolved action with a call to it.
 * The call defines how the actions input arguments are assigned.
 * <p>
 * The equals method can be used on this object to detect if any changes occurred.
 * THis includes changes to the action itself and also changes to the input assignments performed by the call.
 */
@Value
@Builder
public class ActionCallConfig {

    /**
     * The name used for this action call.
     * This corresponds to the data key written by the action.
     */
    private String name;

    /**
     * The input assignments to use for calling the action.
     * It is guaranteed that the action name specified {@link #callSettings} is the name of the action defined by {@link #action}.
     */
    private ActionCallSettings callSettings;

    /**
     * The definition of the action which shall be called.
     * It is guaranteed that the action name specified {@link #callSettings} is the name of the action defined by {@link #action}.
     */
    private GenericActionConfig action;
}

