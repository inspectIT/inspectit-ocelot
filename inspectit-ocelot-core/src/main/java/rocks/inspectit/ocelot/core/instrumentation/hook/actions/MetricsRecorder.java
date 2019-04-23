package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.stats.StatsRecorder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hook action responsible for recording measurements at the exit of an instrumented method
 */
@Value
@Slf4j
public class MetricsRecorder implements IHookAction {

    /**
     * A list of metrics names and the corresponding constant value to record.
     * This is stored as list and not a map because it is faster to iterate over a list than a map.
     */
    private final List<Pair<String, ? extends Number>> constantMetrics = new ArrayList<>();

    /**
     * A list of metric names and corresponding data keys which will be used to find the value for the metric.
     * This is stored as list and not a map because it is faster to iterate over a list than a map.
     */
    private final CopyOnWriteArrayList<Pair<String, String>> dataMetrics = new CopyOnWriteArrayList<>();

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private MeasuresAndViewsManager metricsManager;

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private StatsRecorder statsRecorder;

    public MetricsRecorder(Map<String, ? extends Number> constantMetrics, Map<String, String> dataMetrics, MeasuresAndViewsManager metricsManager, StatsRecorder statsRecorder) {
        constantMetrics.forEach((m, v) -> this.constantMetrics.add(Pair.of(m, v)));
        dataMetrics.forEach((m, v) -> this.dataMetrics.add(Pair.of(m, v)));

        this.metricsManager = metricsManager;
        this.statsRecorder = statsRecorder;
    }

    @Override
    public void execute(ExecutionContext context) {
        try (val ts = context.getInspectitContext().enterFullTagScope()) {
            val measureMap = statsRecorder.newMeasureMap();

            for (val metricAndValue : constantMetrics) {
                metricsManager.tryRecordingMeasurement(metricAndValue.getKey(), measureMap, metricAndValue.getValue());
            }

            for (val measureAndDataKey : dataMetrics) {
                Object value = context.getInspectitContext().getData(measureAndDataKey.getValue());
                //only record metrics where a value is present
                //this allows to disable the recording of a metric depending on the execution results of data providers
                if (value != null) {
                    if (value instanceof Number) {
                        metricsManager.tryRecordingMeasurement(measureAndDataKey.getKey(), measureMap, (Number) value);
                    } else {
                        log.error("The value of data '{}' configured to be used for metric '{}' for method '{}' was not a number!" +
                                        " The recording of this metric is now disabled for this method!",
                                measureAndDataKey.getValue(), measureAndDataKey.getKey(), context.getHook().getMethodInformation().getName());
                        dataMetrics.remove(measureAndDataKey);
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
