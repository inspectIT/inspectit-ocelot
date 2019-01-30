package rocks.inspectit.oce.core.config.model.instrumentation.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedDataProperties;

/**
 * Defines the behaviour of data with a certain key passed around with the {@link rocks.inspectit.oce.core.instrumentation.context.InspectitContext}
 * for instrumented methods. The data key is defined through the map {@link InstrumentationSettings#getData()}.
 */
@Data
@NoArgsConstructor
public class DataSettings {

    /**
     * Defines how data is propagation up within traces.
     * Defaults to {@link PropagationMode#NONE} when not specified.
     * The default values handling is done by {@link rocks.inspectit.oce.core.instrumentation.config.DataPropertiesResolver}
     * as well through the qzery methods of {@link ResolvedDataProperties}.
     */
    private PropagationMode upPropagation;

    /**
     * Defines how data is propagation down within traces.
     * Defaults to {@link PropagationMode#NONE} if the name of the data starts with "local_", otherwise defautls to {@link PropagationMode#JVM_LOCAL}.
     * The default values handling is done by {@link rocks.inspectit.oce.core.instrumentation.config.DataPropertiesResolver}
     * as well through the qzery methods of {@link ResolvedDataProperties}.
     */
    private PropagationMode downPropagation;

    /**
     * Defines whether this datum is visible as an OpenCensus Tag.
     * Defaults to false if the name of the data starts with "local_", otherwise defaults to true.
     * The default values handling is done by {@link rocks.inspectit.oce.core.instrumentation.config.DataPropertiesResolver}
     * as well through the qzery methods of {@link ResolvedDataProperties}.
     */
    private Boolean isTag;

}
