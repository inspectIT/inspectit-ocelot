package rocks.inspectit.ocelot.config.model.selfmonitoring;

import lombok.Data;

/**
 * Settings for the {@link rocks.inspectit.ocelot.core.selfmonitoring.ActionsMetricsRecorder}
 */
@Data
public class ActionMetricsSettings {

    /**
     * Whether metrics of actions (e.g., execution time) are recorded
     */
    private boolean enabled;
}
