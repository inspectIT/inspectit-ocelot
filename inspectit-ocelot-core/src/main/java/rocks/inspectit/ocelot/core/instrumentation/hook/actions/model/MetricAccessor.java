package rocks.inspectit.ocelot.core.instrumentation.hook.actions.model;

import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;

import java.util.Map;

/**
 * Contains all the information needed for recording a single metric value using @{@link VariableAccessor}.
 * <p>
 * Besides the accessor that defines the value, this class wraps name of the metric as well as the constant and data
 * tags that should be recorded with the metric.
 */
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
