package rocks.inspectit.ocelot.core.selfmonitoring;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.selfmonitoring.SelfMonitoringSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;
import rocks.inspectit.ocelot.core.metrics.InstrumentManager;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SelfMonitoringService {

    private static final String METRICS_PREFIX = "inspectit_self_";

    private static final String DURATION_MEASURE_NAME = "duration";

    private static final String COMPONENT_TAG_KEY = "component-name";

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private InstrumentManager instrumentManager;

    @Autowired
    private CommonAttributesManager commonAttributes;

    /**
     * Provides an auto-closable that can be used in try-with-resource form.
     * <p>
     * If self monitoring is enabled the {@link SelfMonitoringScope} instance is created that handles time measuring and metric recording.
     * If self monitoring is disabled, returns a no-ops closable.
     *
     * @param componentName the human-readable name of the component of which the time is measured, is used as attribute value
     *
     * @return the scope performing the metric
     */
    public Scope withDurationSelfMonitoring(String componentName) {
        if (isSelfMonitoringEnabled()) {
            return new SelfMonitoringScope(componentName, System.nanoTime());
        } else {
            return () -> {};
        }
    }

    /**
     * @return true, if the configuration states that self monitoring should be performed
     */
    public boolean isSelfMonitoringEnabled() {
        return env.getCurrentConfig().getSelfMonitoring().isEnabled();
    }

    /**
     * Prints info logs when the configuration changes the self monitoring enabled state.
     *
     * @param ev the config change event
     */
    @EventListener
    private void printInfoOnStateChange(InspectitConfigChangedEvent ev) {
        SelfMonitoringSettings newS = ev.getNewConfig().getSelfMonitoring();
        SelfMonitoringSettings oldS = ev.getOldConfig().getSelfMonitoring();
        if (newS.isEnabled() && !oldS.isEnabled()) {
            log.info("Enabling self monitoring");
        } else if (!newS.isEnabled() && oldS.isEnabled()) {
            log.info("Disabling self monitoring");
        }
    }

    /**
     * Records a self-monitoring metric with the common attributes.
     * The metric has to be defined correctly in the {@link MetricsSettings#getDefinitions()}.
     * Only records a metric if self monitoring is enabled.
     *
     * @param metricName  the name of the metric, excluding the {@link #METRICS_PREFIX}
     * @param value       the actual value
     */
    public void recordMetric(String metricName, double value) {
        SelfMonitoringSettings conf = env.getCurrentConfig().getSelfMonitoring();
        if (conf.isEnabled()) {
            String fullMetricName = METRICS_PREFIX + metricName;
            try (Scope scope = commonAttributes.withCommonAttributesScope()) {
                instrumentManager.tryRecordingMetric(fullMetricName, value);
            }
        }
    }

    /**
     * Records a self-monitoring metric with the common attributes.
     * The measure has to be defined correctly in the {@link MetricsSettings#getDefinitions()}.
     * Only records a metric if self monitoring is enabled.
     *
     * @param metricName  the name of the metric, excluding the {@link #METRICS_PREFIX}
     * @param value       the actual value
     */
    public void recordMetric(String metricName, long value) {
        recordMetric(metricName, value, Collections.emptyMap());
    }

    /**
     * Records a self-monitoring metric with the common attributes. Adds customAttributes to the baggage.
     * The measure has to be defined correctly in the {@link MetricsSettings#getDefinitions()}.
     * Only records a metric if self monitoring is enabled.
     *
     * @param measureName       the name of the metric, excluding the {@link #METRICS_PREFIX}
     * @param value             the actual value
     * @param customAttributes  additional attributes, which are added to the metric
     */
    public void recordMetric(String measureName, long value, Map<String, String> customAttributes) {
        SelfMonitoringSettings conf = env.getCurrentConfig().getSelfMonitoring();
        if (conf.isEnabled()) {
            String fullMetricName = METRICS_PREFIX + measureName;
            try (Scope scope = commonAttributes.withCommonAttributesScope(customAttributes)) {
                instrumentManager.tryRecordingMetric(fullMetricName, value);
            }
        }
    }

    @Data
    private class SelfMonitoringScope implements Scope {

        private final String componentName;

        private final long start;

        @Override
        public void close() {
            double durationInMicros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
            String fullMetricName = METRICS_PREFIX + DURATION_MEASURE_NAME;
            Baggage baggage = commonAttributes.getCommonBaggage()
                    .toBuilder()
                    .put(COMPONENT_TAG_KEY, componentName)
                    .build();

            instrumentManager.tryRecordingMetric(fullMetricName, durationInMicros, baggage);

            if (log.isTraceEnabled()) {
                log.trace(String.format("%s reported %.1f\u00B5s", componentName, durationInMicros));
            }
        }
    }
}
