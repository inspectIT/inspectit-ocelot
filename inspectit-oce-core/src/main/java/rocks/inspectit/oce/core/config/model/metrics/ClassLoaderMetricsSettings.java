package rocks.inspectit.oce.core.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.Map;

/**
 * Settings for the @{@link rocks.inspectit.oce.core.metrics.ClassLoaderMetricsRecorder}.
 */
@Data
@NoArgsConstructor
public class ClassLoaderMetricsSettings {

    /**
     * Contains the enabling flag for each metric.
     */
    private Map<String, Boolean> enabled;

    /**
     * Specifies the frequency in milliseconds with which the disk metrics should be polled and recorded.
     */
    @NonNull
    private Duration frequency;

}
