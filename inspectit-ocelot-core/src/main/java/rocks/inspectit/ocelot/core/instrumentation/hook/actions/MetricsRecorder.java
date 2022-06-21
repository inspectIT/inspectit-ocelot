package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.Tags;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.MeasureTagValueGuard;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;
import rocks.inspectit.ocelot.core.tags.TagUtils;

import java.util.List;
import java.util.Optional;

/**
 * Hook action responsible for recording measurements at the exit of an instrumented method
 */
@Value
@Slf4j
public class MetricsRecorder implements IHookAction {

    /**
     * A list of metric accessors which will be used to find the value and tags for the metric.
     */
    private final List<MetricAccessor> metrics;

    /**
     * Common tags manager needed for gathering common tags when recording metrics.
     */
    private CommonTagsManager commonTagsManager;

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private MeasuresAndViewsManager metricsManager;

    private MeasureTagValueGuard tagValueGuard;

    @Override
    public void execute(ExecutionContext context) {
        // then iterate all metrics and enter new scope for metric collection
        for (MetricAccessor metricAccessor : metrics) {
            Object value = metricAccessor.getVariableAccessor().get(context);
            // only record metrics where a value is present
            // this allows to disable the recording of a metric depending on the results of action executions
            if (value instanceof Number) {
                TagContext tagContext = tagValueGuard.getTagContext(context, metricAccessor);
                metricsManager.tryRecordingMeasurement(metricAccessor.getName(), (Number) value, tagContext);
            }
        }
    }

    @Override
    public String getName() {
        return "Metrics Recorder";
    }
}
