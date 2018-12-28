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
     * Name of the inspectIT self-monitoring measure.
     */
    @NotEmpty
    private String measureName;

}
