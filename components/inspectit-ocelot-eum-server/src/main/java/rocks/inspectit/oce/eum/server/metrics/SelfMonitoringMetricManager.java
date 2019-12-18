package rocks.inspectit.oce.eum.server.metrics;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.common.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.configuration.model.EumSelfMonitoringSettings;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;

/**
 * Central component, which is responsible for recording self monitoring metrics.
 */
@Component
@Slf4j
public class SelfMonitoringMetricManager {

    @Autowired
    private EumServerConfiguration configuration;

    @Autowired
    private MeasuresAndViewsManager measuresAndViewsManager;

    @PostConstruct
    @VisibleForTesting
    void initMetrics() {
        EumSelfMonitoringSettings selfMonitoringSettings = configuration.getSelfMonitoring();
        for (Map.Entry<String, MetricDefinitionSettings> metricEntry : selfMonitoringSettings.getMetrics().entrySet()) {
            String measureName = metricEntry.getKey();
            MetricDefinitionSettings metricDefinitionSettings = metricEntry.getValue();

            String metricName = selfMonitoringSettings.getMetricPrefix() + measureName;
            log.info("Registering self-monitoring metric: {}", metricName);

            measuresAndViewsManager.updateMetrics(metricName, metricDefinitionSettings);
        }
    }

    /**
     * Records a self-monitoring measurement with the common tags.
     * Only records a measurement if self monitoring is enabled.
     *
     * @param measureName the name of the measure, excluding the metrics prefix
     * @param value       the actual value
     * @param customTags  custom tags
     */
    public void record(String measureName, Number value, Map<String, String> customTags) {
        EumSelfMonitoringSettings selfMonitoringSettings = configuration.getSelfMonitoring();
        if (selfMonitoringSettings.isEnabled() && selfMonitoringSettings.getMetrics().containsKey(measureName)) {
            MetricDefinitionSettings metricDefinitionSettings = selfMonitoringSettings.getMetrics().get(measureName);

            String metricName = selfMonitoringSettings.getMetricPrefix() + measureName;

            try (Scope scope = measuresAndViewsManager.getTagContext(customTags).buildScoped()) {
                measuresAndViewsManager.recordMeasure(metricName, metricDefinitionSettings, value);
            }
        }
    }

    /**
     * Records a self-monitoring measurement with the common tags.
     * Only records a measurement if self monitoring is enabled.
     *
     * @param measureName the name of the measure, excluding the metrics prefix
     * @param value       the actual value
     */
    public void record(String measureName, Number value) {
        record(measureName, value, Collections.emptyMap());
    }
}

