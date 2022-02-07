package rocks.inspectit.ocelot.config.model.selfmonitoring;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class SelfMonitoringSettings {

    /**
     * If self-monitoring is enabled.
     */
    private boolean enabled;

    /**
     * Settings for {@link rocks.inspectit.ocelot.core.selfmonitoring.ActionsMetricsRecorder}
     */
    @Valid
    private ActionMetricsSettings actionMetrics;

    /**
     * Settings for the log preloading.
     */
    @Valid
    private LogPreloadingSettings logPreloading;

}
