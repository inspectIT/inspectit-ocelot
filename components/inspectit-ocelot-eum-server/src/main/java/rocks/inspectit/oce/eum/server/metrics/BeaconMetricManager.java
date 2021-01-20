package rocks.inspectit.oce.eum.server.metrics;

import io.opencensus.common.Scope;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.oce.eum.server.arithmetic.RawExpression;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.beacon.recorder.BeaconRecorder;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconMetricDefinitionSettings;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconRequirement;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.utils.TagUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central component, which is responsible for writing beacon entries as OpenCensus views.
 */
@Component
@Slf4j
public class BeaconMetricManager {

    @Autowired
    protected EumServerConfiguration configuration;

    @Autowired
    private MeasuresAndViewsManager measuresAndViewsManager;

    @Autowired(required = false)
    private List<BeaconRecorder> beaconRecorders;

    /**
     * Maps metric definitions to expressions.
     */
    private Map<BeaconMetricDefinitionSettings, RawExpression> expressionCache = new HashMap<>();

    /**
     * Processes boomerang beacon
     *
     * @param beacon The beacon containing arbitrary key-value pairs.
     *
     * @return whether the beacon has been successfully parsed
     */
    public boolean processBeacon(Beacon beacon) {
        boolean successful = false;

        Map<String, BeaconMetricDefinitionSettings> definitions = configuration.getDefinitions();
        if (CollectionUtils.isEmpty(definitions)) {
            successful = true;
        } else {
            for (Map.Entry<String, BeaconMetricDefinitionSettings> metricDefinitionEntry : definitions.entrySet()) {
                String metricName = metricDefinitionEntry.getKey();
                BeaconMetricDefinitionSettings metricDefinition = metricDefinitionEntry.getValue();

                if (BeaconRequirement.validate(beacon, metricDefinition.getBeaconRequirements())) {
                    recordMetric(metricName, metricDefinition, beacon);
                    successful = true;
                } else {
                    log.debug("Skipping beacon because requirements are not fulfilled.");
                }
            }
        }

        // allow each beacon recorder to record stuff
        if (!CollectionUtils.isEmpty(beaconRecorders)) {
            try (Scope scope = getTagContextForBeacon(beacon).buildScoped()) {
                beaconRecorders.forEach(beaconRecorder -> beaconRecorder.record(beacon));
            }
        }

        return successful;
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
    private void recordMetric(String metricName, BeaconMetricDefinitionSettings metricDefinition, Beacon beacon) {
        RawExpression expression = expressionCache.computeIfAbsent(metricDefinition, definition -> new RawExpression(definition
                .getValueExpression()));

        if (expression.isSolvable(beacon)) {
            Number value = expression.solve(beacon);

            if (value != null) {
                measuresAndViewsManager.updateMetrics(metricName, metricDefinition);
                try (Scope scope = getTagContextForBeacon(beacon).buildScoped()) {
                    measuresAndViewsManager.recordMeasure(metricName, metricDefinition, value);
                }
            }
        }
    }

    /**
     * Builds TagContext for a given beacon.
     *
     * @param beacon Used to resolve tag values, which refer to a beacon entry
     */
    private TagContextBuilder getTagContextForBeacon(Beacon beacon) {
        TagContextBuilder tagContextBuilder = measuresAndViewsManager.getTagContext();
        for (String key : configuration.getTags().getBeacon().keySet()) {
            if (beacon.contains(key)) {
                tagContextBuilder.putLocal(TagKey.create(key), TagUtils.createTagValue(key, beacon.get(key)));
            }
        }
        return tagContextBuilder;
    }
}