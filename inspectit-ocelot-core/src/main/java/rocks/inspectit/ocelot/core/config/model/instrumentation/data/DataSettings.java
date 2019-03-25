package rocks.inspectit.ocelot.core.config.model.instrumentation.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContext;

import javax.validation.constraints.NotNull;

/**
 * Defines the behaviour of data with a certain key passed around with the {@link InspectitContext}
 * for instrumented methods. The data key is defined through the map {@link InstrumentationSettings#getData()}.
 */
@Data
@NoArgsConstructor
public class DataSettings {

    /**
     * Defines how data is propagation up within traces.
     */
    @NotNull
    private PropagationMode upPropagation = PropagationMode.NONE;

    /**
     * Defines how data is propagation down within traces.
     */
    @NotNull
    private PropagationMode downPropagation = PropagationMode.JVM_LOCAL;

    /**
     * Defines whether this data is visible as an OpenCensus Tag.
     */
    private boolean isTag = true;

}
