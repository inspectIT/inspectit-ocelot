package rocks.inspectit.oce.eum.server.metrics;

import io.opencensus.common.Scope;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;

import java.util.Collections;
import java.util.Map;

/**
 * Central component, which is responsible for recording self monitoring metrics.
 */
@Component
@Slf4j
public class SelfMonitoringMetricManager extends AbstractMeasuresAndViewsManager {

    private static final String METRICS_PREFIX = "inspectit-eum/self/";

    /**
     * Records a self-monitoring measurement with the common tags.
     * Only records a measurement if self monitoring is enabled.
     *
     * @param measureName the name of the measure, excluding the {@link #METRICS_PREFIX}
     * @param value       the actual value
     * @param customTags  custom tags
     */
    public void record(String measureName, Number value, Map<String, String> customTags) {
        if (configuration.getSelfMonitoring().getMetrics().containsKey(measureName) && configuration.getSelfMonitoring().isEnabled()) {
            MetricDefinitionSettings metricDefinitionSettings = configuration.getSelfMonitoring().getMetrics().get(measureName);
            updateMetrics(METRICS_PREFIX + measureName, metricDefinitionSettings);
            try (Scope scope = getTagContext(customTags).buildScoped()) {
                recordMeasure(METRICS_PREFIX + measureName, metricDefinitionSettings, value);
            }
        }
    }

    /**
     * Records a self-monitoring measurement with the common tags.
     * Only records a measurement if self monitoring is enabled.
     *
     * @param measureName the name of the measure, excluding the {@link #METRICS_PREFIX}
     * @param value       the actual value
     */
    public void record(String measureName, Number value) {
        record(measureName, value, Collections.emptyMap());
    }

    /**
     * Builds TagContext
     */
    protected TagContextBuilder getTagContext(Map<String, String> customTags) {
        TagContextBuilder tagContextBuilder = super.getTagContext();

        for (Map.Entry<String, String> customTag : customTags.entrySet()) {
            tagContextBuilder.putLocal(TagKey.create(customTag.getKey()), TagValue.create(customTag.getValue()));
        }
        return tagContextBuilder;
    }


}

