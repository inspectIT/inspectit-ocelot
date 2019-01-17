package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@NoArgsConstructor
public class InstrumentationSettings {

    /**
     * The number of classes per batch when doing a runtime re-instrumentation
     */
    private int batchSize;

    /**
     * The pause between batches when doing a runtime re-instrumentation
     */
    private Duration interBatchPause;

    /**
     * The configuration for the special sensors.
     */
    private SpecialSettings special;

}
