package rocks.inspectit.oce.core.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.Map;

/**
 * Settings for the @{@link rocks.inspectit.oce.core.metrics.DiskMetricsRecorder}.
 */
@Data
@NoArgsConstructor
public class DiskMetricsSettings {

    /**
     * Contains the enabling flag for each metric.
     */
    Map<String, Boolean> enabled;

    /**
     * Specifies the frequency in milliseconds with which the classloader metrics should be polled and recorded.
     */
    @NonNull
    Duration frequency;

}
