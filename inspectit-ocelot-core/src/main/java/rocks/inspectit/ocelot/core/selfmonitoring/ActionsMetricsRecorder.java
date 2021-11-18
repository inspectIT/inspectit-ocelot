package rocks.inspectit.ocelot.core.selfmonitoring;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.HashMap;

/**
 * Recorder for {@link MethodHook} to record and expose metrics (e.g., number of invocation, execution time) of individual {@link rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction}.
 */
@Component
@Slf4j
public class ActionsMetricsRecorder extends DynamicallyActivatableService {

    @Autowired
    private SelfMonitoringService selfMonitoringService;

    /**
     * The prefix of the recorded metrics.
     */
    private static final String METRIC_NAME_PREFIX = "actions/";

    /**
     * The metric name for the execution time of an action.
     */
    private static final String EXECUTION_TIME_METRIC_NAME = "execution-time";

    /**
     * The metric name for execution count.
     */
    private static final String COUNT_METRIC_NAME = "count";

    /**
     * The key of the action's name used in custom tags.
     */
    private static final String ACTION_NAME_KEY = "action_name";

    public ActionsMetricsRecorder() {
        super("metrics.enabled", "selfMonitoring.actions");
    }

    /**
     * Records the execution time of an {@link IHookAction}.
     *
     * @param action              The action
     * @param executionTimeMicros The execution time in microseconds
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          TODO: do we want to record nano, micro or milliseconds? In case of millis, do we want to store long or double?
     */
    public void record(IHookAction action, long executionTimeMicros) {
        record(action.getName(), executionTimeMicros);
    }

    /**
     * Records the execution time of an action.
     *
     * @param actionName          The name of the execution
     * @param executionTimeMicros The execution time in microseconds
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          TODO: do we want to record nano, micro or milliseconds? In case of millis, do we want to store long or double?
     */
    public void record(String actionName, long executionTimeMicros) {

        // do nothing if this metrics recorder is not enabled
        if (!isEnabled()) {
            return;
        }

        // log.info("recording action '{}', executionTimeMicros = {}", actionName, executionTimeMicros);

        val actionsSettings = env.getCurrentConfig().getSelfMonitoring().getActions();

        // create custom tags
        val customTags = new HashMap<String, String>() {{
            put(ACTION_NAME_KEY, actionName);
        }};

        // record the action's execution time if enabled
        if (actionsSettings.getEnabled().getOrDefault(EXECUTION_TIME_METRIC_NAME, false)) {
            recordMeasurement(EXECUTION_TIME_METRIC_NAME, executionTimeMicros, customTags);
        }

        // record the execution count increment
        if (actionsSettings.getEnabled().getOrDefault(COUNT_METRIC_NAME, false)) {
            recordMeasurement(COUNT_METRIC_NAME, 1L, customTags);
        }

    }

    /**
     * Records the measurement for the specific metric of an action.
     *
     * @param actionName The name of the action
     * @param metricName The name of the metric
     * @param value      The value to be recorded
     */
    private void recordMeasurement(String actionName, String metricName, long value) {
        // return if the recorder or the metric is disabled
        if (!isEnabled() || !env.getCurrentConfig()
                .getSelfMonitoring()
                .getActions()
                .getEnabled()
                .getOrDefault(metricName, false)) {
            return;
        }
        // create custom tags
        val customTags = new HashMap<String, String>() {{
            put(ACTION_NAME_KEY, actionName);
        }};
        // record measurement
        selfMonitoringService.recordMeasurement(METRIC_NAME_PREFIX + metricName, value, customTags);
    }

    /**
     * Records the measurement for the metric.
     *
     * @param metricName The name of the metric
     * @param value      The value to be recorded
     * @param customTags additional tags, which are added to the measurement
     */
    private void recordMeasurement(String metricName, long value, HashMap<String, String> customTags) {
        selfMonitoringService.recordMeasurement(METRIC_NAME_PREFIX + metricName, value, customTags);
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        // check the master switch metrics.enabled, whether self monitoring is enabled and if any metrics in actions.enabled is enabled
        return configuration.getMetrics().isEnabled() && configuration.getSelfMonitoring()
                .isEnabled() && configuration.getSelfMonitoring().getActions().getEnabled().containsValue(true);
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        // TODO: do I need to check whether the InspectitConfig#action settings have been defined at all?
        log.info("Enabling ActionMetricsRecorder.");
        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Disabling ActionMetricsRecorder.");
        return true;
    }
}
