package rocks.inspectit.ocelot.core.instrumentation.hook.actions.model;

import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;

import java.util.Map;

@Value
public class MetricAccessor {

    /**
     * Metric name.
     */
    private final String name;

    /**
     * Metric value variable accessors.
     */
    private final VariableAccessor variableAccessor;

    /**
     * Constant tags keys and values.
     */
    private final Map<String, String> constantTags;

    /**
     * Data tags keys and values.
     */
    private final Map<String, String> dataTags;

}
