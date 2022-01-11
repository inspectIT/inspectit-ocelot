package rocks.inspectit.ocelot.core.selfmonitoring;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.selfmonitoring.ActionMetricsSettings;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.HashMap;

/**
 * Recorder for {@link MethodHook} to record and expose metrics (e.g., number of invocation, execution time) of individual {@link rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction}.
 */
@Component
@Slf4j
public class ActionMetricsRecorder extends DynamicallyActivatableService {

    @Autowired
    private SelfMonitoringService selfMonitoringService;

    /**
     * The prefix of the recorded metrics.
     */
    private static final String METRIC_NAME_PREFIX = "action/";

    /**
     * The metric name for the execution time of an action.
     */
    private static final String EXECUTION_TIME_METRIC_NAME = "execution-time";

    /**
     * The measurement name used for recording.
     */
    private static final String MEASUREMENT_NAME = METRIC_NAME_PREFIX + EXECUTION_TIME_METRIC_NAME;

    /**
     * The key of the action's name used in custom tags.
     */
    private static final String ACTION_NAME_KEY = "action_name";

    public ActionMetricsRecorder() {
        super("metrics.enabled", "selfMonitoring.actionMetrics");
    }

    /**
     * Records the execution time of an action.
     *
     * @param actionName          The name of the execution
     * @param executionTimeMicros The execution time in microseconds
     */
    public void record(String actionName, long executionTimeMicros) {
        if (!isEnabled()) {
            return;
        }

        // create custom tags
        HashMap<String, String> customTags = new HashMap<String, String>() {{
            put(ACTION_NAME_KEY, actionName);
        }};

        // record the action's execution time
        selfMonitoringService.recordMeasurement(MEASUREMENT_NAME, executionTimeMicros, customTags);
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        // check the master switch metrics.enabled, whether self monitoring is enabled and if any metrics in actions.enabled is enabled
        return configuration.getMetrics().isEnabled() && configuration.getSelfMonitoring()
                .isEnabled() && configuration.getSelfMonitoring().getActionMetrics().isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        log.info("Enabling ActionMetricsRecorder.");
        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Disabling ActionMetricsRecorder.");
        return true;
    }
}
