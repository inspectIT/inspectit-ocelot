package rocks.inspectit.ocelot.config.model.instrumentation.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;

/**
 * Defines the behaviour of data with a certain key passed around with the {@link rocks.inspectit.ocelot.core.instrumentation.context.InspectitContext}
 * for instrumented methods. The data key is defined through the map {@link InstrumentationSettings#getData()}.
 */
@Data
@NoArgsConstructor
public class DataSettings {

    /**
     * Defines how data is propagation up within traces.
     */
    private PropagationMode upPropagation;

    /**
     * Defines how data is propagation down within traces.
     */
    private PropagationMode downPropagation;

    /**
     * Defines whether this data is visible as an OpenCensus Tag.
     */
    @Setter(AccessLevel.NONE)
    private Boolean isTag;

    public void setIsTag(Boolean isTag) {
        this.isTag = isTag;
    }

}
