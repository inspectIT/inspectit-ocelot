package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.common.Scope;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;

import java.util.List;

/**
 * Hook action responsible for recording measurements at the exit of an instrumented method
 */
@Value
@Slf4j
public class MetricsRecorder implements IHookAction {

    /**
     * A list of metric names and corresponding variable accessors which will be used to find the value for the metric.
     * This is stored as list instead of a map because the same metric might be written multiple times.
     */
    private final List<Pair<String, VariableAccessor>> metrics;

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private MeasuresAndViewsManager metricsManager;

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private StatsRecorder statsRecorder;

    @Override
    public void execute(ExecutionContext context) {
        try (Scope ts = context.getInspectitContext().enterFullTagScope()) {
            MeasureMap measureMap = statsRecorder.newMeasureMap();

            for (Pair<String, VariableAccessor> measureAndDataKey : metrics) {
                Object value = measureAndDataKey.getValue().get(context);
                //only record metrics where a value is present
                //this allows to disable the recording of a metric depending on the results of action executions
                if (value != null) {
                    if (value instanceof Number) {
                        metricsManager.tryRecordingMeasurement(measureAndDataKey.getKey(), measureMap, (Number) value);
                    }
                }
            }

            measureMap.record();
        }
    }

    @Override
    public String getName() {
        return "Metrics Recorder";
    }
}
