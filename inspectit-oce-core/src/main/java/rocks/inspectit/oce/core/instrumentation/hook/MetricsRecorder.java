package rocks.inspectit.oce.core.instrumentation.hook;

import io.opencensus.stats.Measure;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.oce.core.metrics.MeasuresAndViewsManager;

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
     */
    private List<Pair<String, ? extends Number>> constantMetrics;

    /**
     * A list of metric names and corresponding data keys which will be used to find the value for the metric.
     */
    private CopyOnWriteArrayList<Pair<String, String>> dataMetrics;

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private MeasuresAndViewsManager metricsManager;

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private StatsRecorder statsRecorder;

    public MetricsRecorder(Map<String, ? extends Number> constantMetrics, Map<String, String> dataMetrics, MeasuresAndViewsManager metricsManager, StatsRecorder statsRecorder) {
        this.constantMetrics = new ArrayList<>();
        constantMetrics.forEach((m, v) -> this.constantMetrics.add(Pair.of(m, v)));
        this.dataMetrics = new CopyOnWriteArrayList<>();
        dataMetrics.forEach((m, v) -> this.dataMetrics.add(Pair.of(m, v)));

        this.metricsManager = metricsManager;
        this.statsRecorder = statsRecorder;
    }

    @Override
    public void execute(ExecutionContext context) {
        try (val ts = context.getInspectitContext().enterFullTagScope()) {
            val measureMap = statsRecorder.newMeasureMap();

            for (val metricAndValue : constantMetrics) {
                recordMeasurement(measureMap, metricAndValue.getKey(), metricAndValue.getValue());
            }

            for (val measureAndDataKey : dataMetrics) {
                Object value = context.getInspectitContext().getData(measureAndDataKey.getValue());
                //only record metrics where a value is present
                //this allows to disable the recording of a metric depending on the execution results of data providers
                if (value != null) {
                    if (value instanceof Number) {
                        recordMeasurement(measureMap, measureAndDataKey.getKey(), (Number) value);
                    } else {
                        log.error("The value of data '{}' configured to be used for metric '{}' for method '{}' was not a number!" +
                                        " The recording of this metric is now disabled for this method!",
                                measureAndDataKey.getValue(), measureAndDataKey.getKey(), context.getHook().getMethodName());
                        dataMetrics.remove(measureAndDataKey);
                    }
                }
            }

            measureMap.record();
        }
    }

    private void recordMeasurement(MeasureMap measureMap, String measureName, Number value) {
        val measure = metricsManager.getMeasure(measureName);
        measure.ifPresent(m -> {
            if (m instanceof Measure.MeasureLong) {
                measureMap.put((Measure.MeasureLong) m, value.longValue());
            } else if (m instanceof Measure.MeasureDouble) {
                measureMap.put((Measure.MeasureDouble) m, value.doubleValue());
            }
        });
    }


    @Override
    public String getName() {
        return "Metrics Recorder";
    }
}
