package rocks.inspectit.ocelot.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Common settings for most metrics recorders.
 */
@Data
@NoArgsConstructor
public class StandardMetricsSettings {

    /**
     * Contains the enabling flag for each metric.
     */
    private Map<String, Boolean> enabled;
}
