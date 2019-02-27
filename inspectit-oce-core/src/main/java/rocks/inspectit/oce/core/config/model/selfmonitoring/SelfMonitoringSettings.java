package rocks.inspectit.oce.core.config.model.selfmonitoring;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SelfMonitoringSettings {

    /**
     * If self-monitoring is enabled.
     */
    private boolean enabled;

}
