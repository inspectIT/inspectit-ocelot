package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.tags.*;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.List;
import java.util.Map;
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

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private StatsRecorder statsRecorder;

    @Override
    public void execute(ExecutionContext context) {
        // get all available data from the context and collect in a map
        Map<String, Object> contextTags = context.getInspectitContext().getFullTagMap();

        // then iterate all metrics and enter new scope for metric collection
        for (MetricAccessor metricAccessor : metrics) {
            Object value = metricAccessor.getVariableAccessor().get(context);
            if (value != null) {
                if (value instanceof Number) {
                    // only record metrics where a value is present
                    // this allows to disable the recording of a metric depending on the results of action executions
                    MeasureMap measureMap = statsRecorder.newMeasureMap();
                    metricsManager.tryRecordingMeasurement(metricAccessor.getName(), measureMap, (Number) value);
                    TagContext tagContext = getTagContext(contextTags, metricAccessor);
                    measureMap.record(tagContext);
                }
            }
        }
    }

    private TagContext getTagContext(Map<String, Object> contextTags, MetricAccessor metricAccessor) {
        // create builder
        TagContextBuilder builder = Tags.getTagger().emptyBuilder();

        // first common tags to allow overwrite by constant or data tags
        commonTagsManager.getCommonTagKeys()
                .forEach(commonTagKey -> Optional.ofNullable(contextTags.get(commonTagKey.getName()))
                                .ifPresent(value -> builder.putLocal(commonTagKey, TagValue.create(value.toString())))
                        //TODO if not present in the context do we pull the value from the common tag map
                );

        // then constant tags to allow overwrite by data
        metricAccessor.getConstantTags()
                .forEach((key, value) -> builder.putLocal(TagKey.create(key), TagValue.create(value)));

        // go over data tags and match the value to the key from the contextTags (if available)
        metricAccessor.getDataTags()
                .forEach((key, dataLink) -> Optional.ofNullable(contextTags.get(dataLink))
                        .ifPresent(value -> builder.putLocal(TagKey.create(key), TagValue.create(value.toString())))
                );

        // build and return
        return builder.build();
    }

    @Override
    public String getName() {
        return "Metrics Recorder";
    }
}
