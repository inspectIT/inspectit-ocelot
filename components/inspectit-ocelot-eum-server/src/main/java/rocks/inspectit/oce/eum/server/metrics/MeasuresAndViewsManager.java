package rocks.inspectit.oce.eum.server.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.utils.TagUtils;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central component, which is responsible for writing communication with the OpenCensus.
 */
@Component
@Slf4j
public class MeasuresAndViewsManager {

    /**
     * Measures, which are created.
     */
    private HashMap<String, Measure> metrics = new HashMap<>();

    @Autowired
    private EumServerConfiguration configuration;

    @Autowired
    private StatsRecorder recorder;

    @Autowired
    private ViewManager viewManager;

    /**
     * Records the measure.
     *
     * @param measureName      the name of the measure
     * @param metricDefinition The configuration of the metric, which is activated
     * @param value            The value, which is going to be written.
     */
    public void recordMeasure(String measureName, MetricDefinitionSettings metricDefinition, Number value) {
        if (log.isDebugEnabled()) {
            log.debug("Recording measure '{}' with value '{}'.", measureName, value);
        }

        switch (metricDefinition.getType()) {
            case LONG:
                recorder.newMeasureMap()
                        .put((Measure.MeasureLong) metrics.get(measureName), value.longValue())
                        .record();
                break;
            case DOUBLE:
                recorder.newMeasureMap()
                        .put((Measure.MeasureDouble) metrics.get(measureName), value.doubleValue())
                        .record();
                break;
        }
    }

    /**
     * Updates the metrics
     */
    public void updateMetrics(String name, MetricDefinitionSettings metricDefinition) {
        if (!metrics.containsKey(name)) {
            MetricDefinitionSettings populatedMetricDefinition = metricDefinition.getCopyWithDefaultsPopulated(name);
            Measure measure = createMeasure(name, populatedMetricDefinition);
            metrics.put(name, measure);
            updateViews(name, populatedMetricDefinition);
        }
    }

    private Measure createMeasure(String name, MetricDefinitionSettings metricDefinition) {
        switch (metricDefinition.getType()) {
            case LONG:
                return Measure.MeasureLong.create(name,
                        metricDefinition.getDescription(), metricDefinition.getUnit());
            case DOUBLE:
                return Measure.MeasureDouble.create(name,
                        metricDefinition.getDescription(), metricDefinition.getUnit());
            default:
                throw new RuntimeException("Used measurement type is not supported");
        }
    }

    /**
     * Creates a new {@link View}, if a view for the given metricDefinition was not created, yet.
     *
     * @param metricDefinition the settings of the metric definition
     */
    private void updateViews(String metricName, MetricDefinitionSettings metricDefinition) {
        for (Map.Entry<String, ViewDefinitionSettings> viewDefinitionSettingsEntry : metricDefinition.getViews()
                .entrySet()) {
            String viewName = viewDefinitionSettingsEntry.getKey();
            ViewDefinitionSettings viewDefinitionSettings = viewDefinitionSettingsEntry.getValue();
            if (viewManager.getAllExportedViews().stream().noneMatch(v -> v.getName().asString().equals(viewName))) {
                Aggregation aggregation = createAggregation(viewDefinitionSettings);
                List<TagKey> tagKeys = getTagsForView(viewDefinitionSettings).stream()
                        .map(TagKey::create)
                        .collect(Collectors.toList());
                View view = View.create(View.Name.create(viewName), metricDefinition.getDescription(), metrics.get(metricName), aggregation, tagKeys);
                viewManager.registerView(view);
            }
        }
    }

    /**
     * Returns all tags, which are exposed for the given metricDefinition
     */
    private Set<String> getTagsForView(ViewDefinitionSettings viewDefinitionSettings) {
        Set<String> tags = new HashSet<>(configuration.getTags().getDefineAsGlobal());
        tags.addAll(viewDefinitionSettings.getTags().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()));
        return tags;
    }

    /**
     * Builds TagContext.
     */
    public TagContextBuilder getTagContext() {
        TagContextBuilder tagContextBuilder = Tags.getTagger().currentBuilder();

        for (Map.Entry<String, String> extraTag : configuration.getTags().getExtra().entrySet()) {
            tagContextBuilder.putLocal(TagKey.create(extraTag.getKey()), TagUtils.createTagValue(extraTag.getValue()));
        }

        return tagContextBuilder;
    }

    /**
     * Builds TagContext with custom tags.
     *
     * @param customTags Map containing the custom tags.
     *
     * @return {@link TagContextBuilder} which contains the custom and global (extra) tags
     */
    public TagContextBuilder getTagContext(Map<String, String> customTags) {
        TagContextBuilder tagContextBuilder = getTagContext();

        for (Map.Entry<String, String> customTag : customTags.entrySet()) {
            tagContextBuilder.putLocal(TagKey.create(customTag.getKey()), TagUtils.createTagValue(customTag.getValue()));
        }

        return tagContextBuilder;
    }

    /**
     * Creates an aggregation depending on the given {@link Aggregation}
     */
    private static Aggregation createAggregation(ViewDefinitionSettings viewDefinitionSettings) {
        switch (viewDefinitionSettings.getAggregation()) {
            case COUNT:
                return Aggregation.Count.create();
            case SUM:
                return Aggregation.Sum.create();
            case HISTOGRAM:
                return Aggregation.Distribution.create(BucketBoundaries.create(viewDefinitionSettings.getBucketBoundaries()));
            case LAST_VALUE:
                return Aggregation.LastValue.create();
            default:
                throw new RuntimeException("Unhandled aggregation type: " + viewDefinitionSettings.getAggregation());
        }
    }

}