package rocks.inspectit.ocelot.core.instrumentation.hook.actions.model;

import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;

import java.util.Map;

/**
 * Contains all the information needed for recording a single metric value using @{@link VariableAccessor}.
 * <p>
 * Besides the accessor that defines the value, this class wraps name of the metric as well as the constant and data
 * attributes that should be recorded with the metric.
 */
@Value
public class MetricAccessor {

    /**
     * Metric name.
     */
    String name;

    /**
     * Metric value variable accessors.
     */
    VariableAccessor variableAccessor;

    /**
     * Constant attributes keys and values.
     */
    Map<String, String> constantAttributes;

    /**
     * VariableAccessors for the data attributes.
     */
    Map<String, VariableAccessor> dataAttributeAccessors;

}
