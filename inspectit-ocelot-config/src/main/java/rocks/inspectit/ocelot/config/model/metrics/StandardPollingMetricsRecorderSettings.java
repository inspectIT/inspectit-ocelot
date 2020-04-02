package rocks.inspectit.ocelot.config.model.metrics;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Common settings for polling metrics recorders
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardPollingMetricsRecorderSettings extends StandardMetricsSettings {

    /**
     * Specifies the frequency in milliseconds with which the metrics should be polled and recorded.
     * Should default to ${inspectit.metrics.frequency}
     */
    @NotNull
    private Duration frequency;

}
