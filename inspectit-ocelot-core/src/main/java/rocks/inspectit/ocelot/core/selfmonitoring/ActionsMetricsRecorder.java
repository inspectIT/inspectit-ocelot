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
import java.util.concurrent.TimeUnit;

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

    /**
     * The map containing the actions' start time in nanoseconds.
     */
    private HashMap<String, Long> actionStartTimesNanos = new HashMap<>();

    /**
     * The map containing the actions' execution counts.
     */
    private HashMap<String, Long> actionExecutionCounts = new HashMap<>();

    public ActionsMetricsRecorder() {
        // TODO: do metrics need to be enabled in order to take self-monitoring measurements?
        super("metrics.enabled", "selfMonitoring.actions");
    }

    /**
     * Records the execution time of an {@link IHookAction}.
     *
     * @param action              The action
     * @param executionTimeMicros The execution time in microseconds
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         TODO: do we want to record nano, micro or milliseconds? In case of millis, do we want to store long or double?
     */
    public void record(IHookAction action, long executionTimeMicros) {
        record(action.getName(), executionTimeMicros);
    }

    /**
     * Records the execution time of an action.
     *
     * @param actionName          The name of the execution
     * @param executionTimeMicros The execution time in microseconds
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         TODO: do we want to record nano, micro or milliseconds? In case of millis, do we want to store long or double?
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
        // TODO: do I need to store the count at all? Or is it sufficient to just record the increment (1)?
        // -> I think storing only the increment is sufficient as we will define in the self-monitoring.yml file how we aggregate the '/self/action_count' measures
        if (actionExecutionCounts == null) {
            actionExecutionCounts = new HashMap<>();
        }
        actionExecutionCounts.put(actionName, (actionExecutionCounts.containsKey(actionName) ? actionExecutionCounts.get(actionName) : 0) + 1);

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
        return configuration.getSelfMonitoring().isEnabled() && configuration.getSelfMonitoring()
                .getActions()
                .getEnabled()
                .containsValue(true);
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
        // clear the map of actionStartTimes
        actionStartTimesNanos.clear();
        return true;
    }

    // probably useless code

    /**
     * Records the start of the execution of an action.
     *
     * @param actionName The name of the action
     */
    private void recordStart(String actionName) {
        // do nothing if disabled
        if (!isEnabled()) {
            return;
        }

        if (actionStartTimesNanos == null) {
            actionStartTimesNanos = new HashMap<>();
        }

        actionStartTimesNanos.put(actionName, System.nanoTime());
    }

    /**
     * Records the end of the execution of an action.
     *
     * @param actionName The name of the action
     */
    private void recordEnd(String actionName) {
        // do nothing if disabled
        if (!isEnabled()) {
            return;
        }

        val actionStartTimeNanos = actionStartTimesNanos.get(actionName);
        // if the ActionMetricsRecorder has been disabled during the execution of an action, the actionStartTimes has been cleared and no actionStartTime could be retrieved.
        if (actionStartTimeNanos == null) {
            return;
        }

        // compute execution time in microseconds
        val executionTimeMicros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - actionStartTimeNanos);

        // remove the action from the start map
        actionStartTimesNanos.remove(actionName);

        // record the execution time
        record(actionName, executionTimeMicros);
    }
}
