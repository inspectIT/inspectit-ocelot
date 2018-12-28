package rocks.inspectit.oce.core.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.Map;

/**
 * Common settings for polling metrics recorders
 */
@Data
@NoArgsConstructor
public class StandardPollingMetricsRecorderSettings {

    /**
     * Contains the enabling flag for each metric.
     */
    private Map<String, Boolean> enabled;

    /**
     * Specifies the frequency in milliseconds with which the metrics should be polled and recorded.
     * Should default to ${inspectit.metrics.frequency}
     */
    @NonNull
    private Duration frequency;

}
