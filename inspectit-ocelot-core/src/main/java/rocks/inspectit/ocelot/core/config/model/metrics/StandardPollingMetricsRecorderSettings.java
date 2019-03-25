package rocks.inspectit.ocelot.core.config.model.metrics;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

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
    @NonNull
    private Duration frequency;

}
