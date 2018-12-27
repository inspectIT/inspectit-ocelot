package rocks.inspectit.oce.core.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MetricsSettings {

    /**
     * Master switch for disabling metrics capturing and exporting.
     * If disabled the following happens:
     * - all metrics exporters are disabled
     */
    boolean enabled;
}
