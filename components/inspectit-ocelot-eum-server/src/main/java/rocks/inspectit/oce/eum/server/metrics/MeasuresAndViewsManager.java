package rocks.inspectit.oce.eum.server.metrics;

import io.opencensus.common.Scope;
import io.opencensus.stats.*;
import io.opencensus.tags.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.model.config.BeaconMetricDefinition;
import rocks.inspectit.oce.eum.server.model.config.Configuration;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central component, which is responsible for writing beacon entries as OpenCensus views.
 */
@Component
public class MeasuresAndViewsManager {
    /**
     * Measures, which are created.
     */
    HashMap<String, Measure> metrics = new HashMap<>();

    @Autowired
    private Configuration configuration;

    /**
     * Processes boomerang beacon
     *
     * @param beacon The beacon containing arbitrary key-value pairs.
     */
    public void processBeacon(Map<String, String> beacon) {
        for (Map.Entry<String, BeaconMetricDefinition> metricDefinition : configuration.getDefinitions().entrySet()) {
            metricDefinition.setValue(metricDefinition.getValue().getCopyWithDefaultsPopulated(metricDefinition.getKey()));
            if (beacon.containsKey(metricDefinition.getValue().getBeaconField())) {
                updateMetrics(metricDefinition.getKey(), metricDefinition.getValue());
                try (Scope scope = getTagContext(beacon).buildScoped()) {
                    recordMeasure(metricDefinition.getKey(),metricDefinition.getValue(), beacon.get(metricDefinition.getValue().getBeaconField()));
                }
            }
        }
    }

    /**
     * Records the measure,
     * @param name
     * @param metricDefinition The configuration of the metric, which is activated
     * @param value            The value, which is going to be written.
     */
    private void recordMeasure(String name, BeaconMetricDefinition metricDefinition, String value) {
        StatsRecorder recorder = Stats.getStatsRecorder();
            switch (metricDefinition.getType()) {
                case LONG:
                        recorder.newMeasureMap().put((Measure.MeasureLong) metrics.get(name), Long.parseLong(value)).record();
                    break;
                case DOUBLE:
                        recorder.newMeasureMap().put((Measure.MeasureDouble) metrics.get(name), Double.parseDouble(value)).record();
                    break;
        }
    }

    /**
     * Updates the metrics
     * @param name
     * @param metricDefinition
     */
    private void updateMetrics(String name, BeaconMetricDefinition metricDefinition) {
        if (!metrics.containsKey(name)) {
            Measure measure = createMeasure(name, metricDefinition);
            metrics.put(name, measure);
            updateViews(name, metricDefinition);
        }
    }

    private Measure createMeasure(String name, BeaconMetricDefinition metricDefinition) {
        switch(metricDefinition.getType()){
            case LONG:
                return Measure.MeasureLong.create(name,
                        metricDefinition.getDescription(), metricDefinition.getUnit());
            case DOUBLE:
                return Measure.MeasureDouble.create(name,
                        metricDefinition.getDescription() , metricDefinition.getUnit());
            default:
                throw new RuntimeException("Used measurement type is not supported");
        }
    }

    public boolean isParsable(String measurement, MetricDefinitionSettings.MeasureType type) {
        switch(type){
            case LONG:
                try{
                    java.lang.Long.parseLong(measurement);
                    return true;
                } catch (NumberFormatException e){
                    return false;
                }
            case DOUBLE:
                try{
                    java.lang.Double.parseDouble(measurement);
                    return true;
                } catch (NumberFormatException e){
                    return false;
                }
            default:
                return false;
        }
    }

    /**
     * Creates a new {@link View}, if a view for the given metricDefinition was not created, yet.
     *
     * @param metricDefinition
     */
    private void updateViews(String metricName, BeaconMetricDefinition metricDefinition) {
        ViewManager viewManager = Stats.getViewManager();

        for (Map.Entry<String,ViewDefinitionSettings> viewDefinitonSettings : metricDefinition.getViews().entrySet()) {
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
        Set<String> tags = new HashSet<>(configuration.getTags().getGlobal());
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
    private TagContextBuilder getTagContext(Map<String, String> beacon) {
        TagContextBuilder tagContextBuilder = Tags.getTagger().currentBuilder();

        for(Map.Entry<String, String> extraTag : configuration.getTags().getExtra().entrySet()){
            tagContextBuilder.put(TagKey.create(extraTag.getKey()), TagValue.create(extraTag.getValue()), TagMetadata.create(TagMetadata.TagTtl.NO_PROPAGATION));
        }

        for(Map.Entry<String, String> beaconTag : configuration.getTags().getBeacon().entrySet()) {
            if (beacon.containsKey(beaconTag.getValue())) {
                tagContextBuilder.put(TagKey.create(beaconTag.getKey()), TagValue.create(beacon.get(beaconTag.getValue())), TagMetadata.create(TagMetadata.TagTtl.NO_PROPAGATION));
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