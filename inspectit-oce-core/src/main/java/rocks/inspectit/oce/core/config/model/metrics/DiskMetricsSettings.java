package rocks.inspectit.oce.core.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;

/**
 * Settings for the @{@link rocks.inspectit.oce.core.metrics.DiskMetricsRecorder}.
 */
@Data
@NoArgsConstructor
public class DiskMetricsSettings {

    /**
     * if true, the free disk space will be measured and the view "disk/free" is registered.
     */
    boolean free;

    /**
     * if true, the total disk space will be measured and the view "disk/total" is registered.
     */
    boolean total;

    /**
     * Specifies the frequency in milliseconds with which the disk metrics should be polled and recorded.
     */
    @Min(1)
    int frequencyMs;

}
