package rocks.inspectit.oce.core.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class GCMetricsSettings {

    /**
     * Contains the enabling flag for each metric.
     */
    private Map<String, Boolean> enabled;
}
