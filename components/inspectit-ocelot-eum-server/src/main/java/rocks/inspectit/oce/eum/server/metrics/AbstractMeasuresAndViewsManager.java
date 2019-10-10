package rocks.inspectit.oce.eum.server.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central component, which is responsible for writing communication with the OpenCensus.
 */
@Slf4j
public abstract class AbstractMeasuresAndViewsManager {

    /**
     * Measures, which are created.
     */
    protected HashMap<String, Measure> metrics = new HashMap<>();

    @Autowired
    protected EumServerConfiguration configuration;

    @Autowired
    private StatsRecorder recorder;

    @Autowired
    private ViewManager viewManager;


    /**
     * Records the measure,
     *
     * @param measureName      the name of the measure
     * @param metricDefinition The configuration of the metric, which is activated
     * @param value            The value, which is going to be written.
     */
    protected void recordMeasure(String measureName, MetricDefinitionSettings metricDefinition, Number value) {
        if (log.isDebugEnabled()) {
            log.debug("Recording measure '{}' with value '{}'.", measureName, value);
        }

        switch (metricDefinition.getType()) {
            case LONG:
                recorder.newMeasureMap().put((Measure.MeasureLong) metrics.get(measureName), value.longValue()).record();
                break;
            case DOUBLE:
                recorder.newMeasureMap().put((Measure.MeasureDouble) metrics.get(measureName), value.doubleValue()).record();
                break;
        }
    }

    /**
     * Updates the metrics
     */
    protected void updateMetrics(String name, MetricDefinitionSettings metricDefinition) {
        if (!metrics.containsKey(name)) {
            MetricDefinitionSettings populatedMetricDefinition = metricDefinition.getCopyWithDefaultsPopulated(name);
            Measure measure = createMeasure(name, populatedMetricDefinition);
            metrics.put(name, measure);
            updateViews(name, populatedMetricDefinition);
        }
    }

    protected Measure createMeasure(String name, MetricDefinitionSettings metricDefinition) {
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
     * @param metricDefinition
     */
    protected void updateViews(String metricName, MetricDefinitionSettings metricDefinition) {
        for (Map.Entry<String, ViewDefinitionSettings> viewDefinitionSettingsEntry : metricDefinition.getViews().entrySet()) {
            String viewName = viewDefinitionSettingsEntry.getKey();
            ViewDefinitionSettings viewDefinitionSettings = viewDefinitionSettingsEntry.getValue();
            if (viewManager.getAllExportedViews().stream().noneMatch(v -> v.getName().asString().equals(viewName))) {
                Aggregation aggregation = createAggregation(viewDefinitionSettings);
                List<TagKey> tagKeys = getTagsForView(viewDefinitionSettings).stream()
                        .map(tag -> TagKey.create(tag))
                        .collect(Collectors.toList());
                View view = View.create(View.Name.create(viewName), metricDefinition.getDescription(), metrics.get(metricName), aggregation, tagKeys);
                viewManager.registerView(view);
            }
        }
    }

    /**
     * Returns all tags, which are exposed for the given metricDefinition
     *
     * @param viewDefinitionSettings
     * @return Map of tags
     */
    protected Set<String> getTagsForView(ViewDefinitionSettings viewDefinitionSettings) {
        Set<String> tags = new HashSet<>(configuration.getTags().getDefineAsGlobal());
        tags.addAll(viewDefinitionSettings.getTags().entrySet().stream()
                .filter(entry -> entry.getValue())
                .map(entry -> entry.getKey())
                .collect(Collectors.toList()));
        return tags;
    }

    /**
     * Builds TagContext
     */
    protected TagContextBuilder getTagContext() {
        TagContextBuilder tagContextBuilder = Tags.getTagger().currentBuilder();

        for (Map.Entry<String, String> extraTag : configuration.getTags().getExtra().entrySet()) {
            tagContextBuilder.putLocal(TagKey.create(extraTag.getKey()), TagValue.create(extraTag.getValue()));
        }
        return tagContextBuilder;
    }

    /**
     * Creates an aggregation depending on the given {@link Aggregation}
     *
     * @param viewDefinitionSettings
     * @return the aggregation
     */
    protected static Aggregation createAggregation(ViewDefinitionSettings viewDefinitionSettings) {
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