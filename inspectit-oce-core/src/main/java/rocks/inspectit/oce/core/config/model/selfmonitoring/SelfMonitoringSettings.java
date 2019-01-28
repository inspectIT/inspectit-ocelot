package rocks.inspectit.oce.core.config.model.selfmonitoring;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
public class SelfMonitoringSettings {

    /**
     * If self-monitoring is enabled.
     */
    private boolean enabled;

    /**
     * Prefix for all self-monitoring measures and views.
     */
    @NotEmpty
    private String measurePrefix;

}
