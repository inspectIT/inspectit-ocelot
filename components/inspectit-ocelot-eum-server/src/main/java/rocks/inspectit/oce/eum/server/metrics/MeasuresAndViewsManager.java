package rocks.inspectit.oce.eum.server.metrics;

import io.opencensus.common.Scope;
import io.opencensus.stats.*;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.arithmetic.RawExpression;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconMetricDefinition;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconRequirement;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.utils.DefaultTags;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central component, which is responsible for writing beacon entries as OpenCensus views.
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
     * Maps metric definitions to expressions.
     */
    private Map<BeaconMetricDefinition, RawExpression> expressionCache = new HashMap<>();

    /**
     * Processes boomerang beacon
     *
     * @param beacon The beacon containing arbitrary key-value pairs.
     */
    public void processBeacon(Beacon beacon) {
        for (Map.Entry<String, BeaconMetricDefinition> metricDefinitionEntry : configuration.getDefinitions().entrySet()) {
            String metricName = metricDefinitionEntry.getKey();
            BeaconMetricDefinition metricDefinition = metricDefinitionEntry.getValue();

            if (BeaconRequirement.validate(beacon, metricDefinition.getBeaconRequirements())) {
                recordMetric(metricName, metricDefinition, beacon);
            } else {
                log.debug("Skipping beacon because requirements are not fulfilled.");
            }
        }
    }

    /**
     * Extracts the metric value from the given beacon according to the specified metric definition.
     * In case the metric definition's value expression is not solvable using the given beacon (not all required
     * fields are existing) nothing is done.
     *
     * @param metricName       the metric name
     * @param metricDefinition the metric's definition
     * @param beacon           the current beacon
     */
    private void recordMetric(String metricName, BeaconMetricDefinition metricDefinition, Beacon beacon) {
        RawExpression expression = expressionCache.computeIfAbsent(metricDefinition, definition -> new RawExpression(definition.getValueExpression()));

        if (expression.isSolvable(beacon)) {
            Number value = expression.solve(beacon);

            updateMetrics(metricName, metricDefinition);
            try (Scope scope = getTagContext(beacon).buildScoped()) {
                recordMeasure(metricName, metricDefinition, value);
            }
        }
    }

    /**
     * Records the measure,
     *
     * @param measureName
     * @param metricDefinition The configuration of the metric, which is activated
     * @param value            The value, which is going to be written.
     */
    private void recordMeasure(String measureName, BeaconMetricDefinition metricDefinition, Number value) {
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
     *
     * @param name
     * @param metricDefinition
     */
    private void updateMetrics(String name, BeaconMetricDefinition metricDefinition) {
        if (!metrics.containsKey(name)) {
            BeaconMetricDefinition populatedMetricDefinition = metricDefinition.getCopyWithDefaultsPopulated(name);
            Measure measure = createMeasure(name, populatedMetricDefinition);
            metrics.put(name, measure);
            updateViews(name, populatedMetricDefinition);
        }
    }

    private Measure createMeasure(String name, BeaconMetricDefinition metricDefinition) {
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
    private void updateViews(String metricName, BeaconMetricDefinition metricDefinition) {
        for (Map.Entry<String, ViewDefinitionSettings> viewDefinitonSettings : metricDefinition.getViews().entrySet()) {
            String viewName = viewDefinitonSettings.getKey();
            ViewDefinitionSettings viewDefinitionSettings = viewDefinitonSettings.getValue();
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
    private Set<String> getTagsForView(ViewDefinitionSettings viewDefinitionSettings) {
        Set<String> tags = new HashSet<>(configuration.getTags().getDefineAsGlobal());
        tags.addAll(viewDefinitionSettings.getTags().entrySet().stream()
                .filter(entry -> entry.getValue())
                .map(entry -> entry.getKey())
                .collect(Collectors.toList()));
        return tags;
    }

    /**
     * Builds TagContext
     *
     * @param beacon Used to resolve tag values, which refer to a beacon entry
     */
    private TagContextBuilder getTagContext(Beacon beacon) {
        TagContextBuilder tagContextBuilder = Tags.getTagger().currentBuilder();

        for (Map.Entry<String, String> extraTag : configuration.getTags().getExtra().entrySet()) {
            tagContextBuilder.putLocal(TagKey.create(extraTag.getKey()), TagValue.create(extraTag.getValue()));
        }

        for (Map.Entry<String, String> beaconTag : configuration.getTags().getBeacon().entrySet()) {
            if (beacon.contains(beaconTag.getValue())) {
                tagContextBuilder.putLocal(TagKey.create(beaconTag.getKey()), TagValue.create(beacon.get(beaconTag.getValue())));
            }
        }

        for (DefaultTags defaultTag : DefaultTags.values()) {
            if (beacon.contains(defaultTag.name())) {
                tagContextBuilder.putLocal(TagKey.create(defaultTag.name()), TagValue.create(beacon.get(defaultTag.name())));
            }
        }

        return tagContextBuilder;
    }

    /**
     * Creates an aggregation depending on the given {@link Aggregation}
     *
     * @param viewDefinitionSettings
     * @return the aggregation
     */
    private Aggregation createAggregation(ViewDefinitionSettings viewDefinitionSettings) {
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