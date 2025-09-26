package rocks.inspectit.ocelot.core.instrumentation.hook.actions.metrics;

import io.opentelemetry.api.baggage.Baggage;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.InstrumentManager;
import rocks.inspectit.ocelot.core.metrics.MetricTagValueGuard;

import java.util.List;

/**
 * Hook action responsible for recording metrics at the exit of an instrumented method
 */
@Value
@Slf4j
public class MetricsRecorder implements IHookAction {

    /**
     * A list of metric accessors which will be used to find the value and tags for the metric.
     */
    List<MetricAccessor> metrics;

    /**
     * Provides baggage, overwrites tag values if they exceed their configured limit.
     */
    MetricTagValueGuard tagValueGuard;

    /**
     * The manager to acquire the actual OpenTelemetry instruments from.
     */
    InstrumentManager instrumentManager;

    @Override
    public void execute(ExecutionContext context) {
        // then iterate all metrics and enter new scope for metric collection
        for (MetricAccessor metricAccessor : metrics) {
            Object value = metricAccessor.getVariableAccessor().get(context);
            // only record metrics where a value is present
            // this allows to disable the recording of a metric depending on the results of action executions
            if (value instanceof Number) {
                Baggage baggage = tagValueGuard.getBaggage(context, metricAccessor);
                instrumentManager.tryRecordingMetric(metricAccessor.getName(), (Number) value, baggage);
            }
        }
    }

    @Override
    public String getName() {
        return "Metrics Recorder";
    }
}
