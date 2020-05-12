package rocks.inspectit.ocelot.core.instrumentation.hook.actions.model;

import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;

import java.util.Map;

/**
 * Contains the information used to generate events.
 */
@Value
public class EventAccessor {
    /**
     * Event name.
     */
    private final String name;

    /**
     * Event attributes including event variables.
     */
    private final Map<String, Object> attributes;

    /**
     * Constant tags keys and values.
     */
    private final Map<String, String> constantTags;

    /**
     * VariableAccessors for the variables.
     */
    private final Map<String, VariableAccessor> variableAccessors;
}
