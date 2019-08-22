package rocks.inspectit.ocelot.core.selfmonitoring;

import io.opencensus.common.Scope;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.selfmonitoring.SelfMonitoringSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SelfMonitoringService {

    private static final String METRICS_PREFIX = "inspectit/self/";

    private static final String DURATION_MEASURE_NAME = "duration";

    private static final TagKey COMPONENT_TAG_KEY = TagKey.create("component-name");


    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private StatsRecorder statsRecorder;

    @Autowired
    private MeasuresAndViewsManager measureManager;

    @Autowired
    private CommonTagsManager commonTags;


    /**
     * Provides an auto-closable that can be used in try-with-resource form.
     * <p>
     * If self monitoring is enabled the {@link SelfMonitoringScope} instance is create that handles time measuring and measure recording.
     * If self monitoring is disabled, returns a no-ops closable.
     *
     * @param componentName the human readable name of the component of which the time is measured, is used as tag value
     * @return the scope performing the measurement
     */
    public Scope withDurationSelfMonitoring(String componentName) {
        if (isSelfMonitoringEnabled()) {
            return new SelfMonitoringScope(componentName, System.nanoTime());
        } else {
            return () -> {
            };
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
            log.info("Enabling self monitoring.");
        } else if (!newS.isEnabled() && oldS.isEnabled()) {
            log.info("Disabling self monitoring.");
        }
    }

    /**
     * Records a self-monitoring measurement with the common tags.
     * The measure has to be defined correctly in the {@link MetricsSettings#getDefinitions()}.
     * Only records a measurement if self monitoring is enabled.
     *
     * @param measureName the name of the measure, excluding the {@link #METRICS_PREFIX}
     * @param value       the actual value
     */
    public void recordMeasurement(String measureName, double value) {
        SelfMonitoringSettings conf = env.getCurrentConfig().getSelfMonitoring();
        if (conf.isEnabled()) {
            String fullMeasureName = METRICS_PREFIX + measureName;
            val measure = measureManager.getMeasureDouble(fullMeasureName);
            measure.ifPresent(m -> {
                try (val ct = commonTags.withCommonTagScope()) {
                    statsRecorder.newMeasureMap()
                            .put(m, value)
                            .record();
                }
            });
        }
    }

    /**
     * Records a self-monitoring measurement with the common tags.
     * The measure has to be defined correctly in the {@link MetricsSettings#getDefinitions()}.
     * Only records a measurement if self monitoring is enabled.
     *
     * @param measureName the name of the measure, excluding the {@link #METRICS_PREFIX}
     * @param value       the actual value
     */
    public void recordMeasurement(String measureName, long value) {
       recordMeasurement(measureName, value, Collections.emptyMap());
    }

    /**
     * Records a self-monitoring measurement with the common tags. Adds customTags to the tag context.
     * The measure has to be defined correctly in the {@link MetricsSettings#getDefinitions()}.
     * Only records a measurement if self monitoring is enabled.
     *
     * @param measureName the name of the measure, excluding the {@link #METRICS_PREFIX}
     * @param value       the actual value
     * @param customTags  additional tags, which are added to the measurement.
     */
    public void recordMeasurement(String measureName, long value, Map<String, String> customTags) {
        SelfMonitoringSettings conf = env.getCurrentConfig().getSelfMonitoring();
        if (conf.isEnabled()) {
            String fullMeasureName = METRICS_PREFIX + measureName;
            val measure = measureManager.getMeasureLong(fullMeasureName);
            measure.ifPresent(m -> {
                try (val ct = commonTags.withCommonTagScope(customTags)) {
                    statsRecorder.newMeasureMap()
                            .put(m, value)
                            .record();
                }
            });
        }
    }

    @Data
    public class SelfMonitoringScope implements Scope {

        private final String componentName;
        private final long start;

        @Override
        public void close() {
            double durationInMicros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
            val measure = measureManager.getMeasureDouble(METRICS_PREFIX + DURATION_MEASURE_NAME);
            measure.ifPresent(m ->
                    statsRecorder.newMeasureMap()
                            .put(m, durationInMicros)
                            .record(Tags.getTagger().toBuilder(commonTags.getCommonTagContext())
                                    .put(COMPONENT_TAG_KEY, TagValue.create(componentName)).build())
            );

            if (log.isTraceEnabled()) {
                log.trace(String.format("%s reported %.1fμs", componentName, durationInMicros));
            }
        }
    }


}
