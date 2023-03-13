package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.Tags;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
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

    @Override
    public void execute(ExecutionContext context) {
        // then iterate all metrics and enter new scope for metric collection
        for (MetricAccessor metricAccessor : metrics) {
            Object value = metricAccessor.getVariableAccessor().get(context);
            if (value instanceof Number) {
                // only record metrics where a value is present
                // this allows to disable the recording of a metric depending on the results of action executions
                TagContext tagContext = getTagContext(context, metricAccessor);
                metricsManager.tryRecordingMeasurement(metricAccessor.getName(), (Number) value, tagContext);

                // OTEL
                metricsManager.tryRecordingMeasurement(metricAccessor.getName(), (Number) value, getAttributes(context, metricAccessor));
            }
        }
    }

    private TagContext getTagContext(ExecutionContext context, MetricAccessor metricAccessor) {
        InspectitContextImpl inspectitContext = context.getInspectitContext();

        // create builder
        TagContextBuilder builder = Tags.getTagger().emptyBuilder();

        // first common tags to allow to overwrite by constant or data tags
        commonTagsManager.getCommonTagKeys()
                .forEach(commonTagKey -> Optional.ofNullable(inspectitContext.getData(commonTagKey.getName()))
                        .ifPresent(value -> builder.putLocal(commonTagKey, TagUtils.createTagValue(commonTagKey.getName(), value.toString()))));

        // then constant tags to allow to overwrite by data
        metricAccessor.getConstantTags()
                .forEach((key, value) -> builder.putLocal(TagKey.create(key), TagUtils.createTagValue(key, value)));

        // go over data tags and match the value to the key from the contextTags (if available)
        metricAccessor.getDataTagAccessors()
                .forEach((key, accessor) -> Optional.ofNullable(accessor.get(context))
                        .ifPresent(tagValue -> builder.putLocal(TagKey.create(key), TagUtils.createTagValue(key, tagValue.toString()))));

        // build and return
        return builder.build();
    }

    private Attributes getAttributes(ExecutionContext context, MetricAccessor metricAccessor) {
        InspectitContextImpl inspectitContext = context.getInspectitContext();

        AttributesBuilder builder = Attributes.builder();

        // TODO: do we need to have a type differentiation for the value to, e.g., type the key in StringKey, LongKey etc.?
        // first common tags to allow to overwrite by constant or data tags
        commonTagsManager.getCommonAttributeKeys()
                .forEach(key -> Optional.ofNullable(inspectitContext.getData(key.getKey()))
                        .ifPresent(value -> builder.put(key, TagUtils.createAttributeValue(key, value.toString()))));

        // then constant tags to allow to overwrite by data
        metricAccessor.getConstantTags()
                .forEach((key, value) -> builder.put(AttributeKey.stringKey(key), TagUtils.createAttributeValue(key, value)));

        // go over data tags and match the value to the key from the contextTags (if available)
        metricAccessor.getDataTagAccessors()
                .forEach((key, accessor) -> Optional.ofNullable(accessor.get(context))
                        .ifPresent(value -> builder.put(AttributeKey.stringKey(key), TagUtils.createAttributeValue(key, value.toString()))));

        // TODO: remove this debug attribute later
        builder.put(AttributeKey.stringKey("debug"), "otel-metric");

        // build and return
        return builder.build();
    }

    @Override
    public String getName() {
        return "Metrics Recorder";
    }
}
