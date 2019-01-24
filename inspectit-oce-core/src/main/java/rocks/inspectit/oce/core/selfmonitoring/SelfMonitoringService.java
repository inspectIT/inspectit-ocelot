package rocks.inspectit.oce.core.selfmonitoring;

import io.opencensus.common.Scope;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
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
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.selfmonitoring.SelfMonitoringSettings;
import rocks.inspectit.oce.core.metrics.MeasuresAndViewsProvider;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
public class SelfMonitoringService {

    private static final String DURATION_MEASURE_NAME = "duration";
    private static final String DURATION_MEASURE_DESCRIPTION = "inspectIT OCE self-monitoring duration";
    private static final String DURATION_MEASURE_UNIT = "μs";

    private static final TagKey COMPONENT_TAG_KEY = TagKey.create("component-name");


    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private StatsRecorder statsRecorder;

    @Autowired
    private MeasuresAndViewsProvider measureProvider;

    @Autowired
    private CommonTagsManager commonTags;


    /**
     * Provides an auto-closable that can be used in try-with-resource form.
     * <p>
     * If self monitoring is enabled the {@link SelfMonitoringScope} instance is create that handles time measuring and measure recording.
     * If self monitoring is disabled, returns a no-ops closable.
     *
     * @param componentName
     * @return
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
     * Utility function to record a measurement with the common tags.
     *
     * @param measure the measure, derived via {@link #getSelfMonitoringMeasureLong(String, String, String, Supplier, TagKey...)}
     * @param value   the actual value
     */
    public void recordMeasurement(Measure.MeasureLong measure, long value) {
        try (val ct = commonTags.withCommonTagScope()) {
            statsRecorder.newMeasureMap()
                    .put(measure, value)
                    .record();
        }
    }

    /**
     * Utility function to record a measurement with the common tags.
     *
     * @param measure the measure, derived via {@link #getSelfMonitoringMeasureDouble(String, String, String, Supplier, TagKey...)}
     * @param value   the actual value
     */
    public void recordMeasurement(Measure.MeasureDouble measure, double value) {
        try (val ct = commonTags.withCommonTagScope()) {
            statsRecorder.newMeasureMap()
                    .put(measure, value)
                    .record();
        }
    }

    /**
     * Gets or creates a new self monitoring measure and view with the given name.
     * The name is prefixed with the configured self monitoring prefix and all common tags are added to the view.
     *
     * @param measureName        the name of the measure, will be prefixed with {@link SelfMonitoringSettings#getMeasurePrefix()}
     * @param description        the description of the measure and view
     * @param unit               the unit of the measure
     * @param aggregation        the aggregation to use for the view
     * @param additionalViewTags additional tags to add to the view
     * @return the created or existing measure
     */
    public Measure.MeasureLong getSelfMonitoringMeasureLong(String measureName, String description, String unit, Supplier<Aggregation> aggregation, TagKey... additionalViewTags) {
        String fullName = env.getCurrentConfig().getSelfMonitoring().getMeasurePrefix() + measureName;
        return measureProvider.getOrCreateMeasureLongWithViewAndCommonTags(
                fullName, description, unit, aggregation, additionalViewTags);

    }

    /**
     * Gets or creates a new self monitoring measure and view with the given name.
     * The name is prefixed with the configured self monitoring prefix and all common tags are added to the view.
     *
     * @param measureName        the name of the measure, will be prefixed with {@link SelfMonitoringSettings#getMeasurePrefix()}
     * @param description        the description of the measure and view
     * @param unit               the unit of the measure
     * @param aggregation        the aggregation to use for the view
     * @param additionalViewTags additional tags to add to the view
     * @return the created or existing measure
     */
    public Measure.MeasureDouble getSelfMonitoringMeasureDouble(String measureName, String description, String unit, Supplier<Aggregation> aggregation, TagKey... additionalViewTags) {
        String fullName = env.getCurrentConfig().getSelfMonitoring().getMeasurePrefix() + measureName;
        return measureProvider.getOrCreateMeasureDoubleWithViewAndCommonTags(
                fullName, description, unit, aggregation, additionalViewTags);
    }

    private Measure.MeasureDouble getSelfMonitoringDurationMeasure() {
        return getSelfMonitoringMeasureDouble(DURATION_MEASURE_NAME, DURATION_MEASURE_DESCRIPTION, DURATION_MEASURE_UNIT, Aggregation.Sum::create, COMPONENT_TAG_KEY);
    }

    @Data
    public class SelfMonitoringScope implements Scope {

        private final String componentName;
        private final long start;

        @Override
        public void close() {
            double durationInMicros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
            statsRecorder.newMeasureMap()
                    .put(getSelfMonitoringDurationMeasure(), durationInMicros)
                    .record(Tags.getTagger().toBuilder(commonTags.getCommonTagContext())
                            .put(COMPONENT_TAG_KEY, TagValue.create(componentName)).build());


            if (log.isTraceEnabled()) {
                log.trace(String.format("%s reported %.1fμs", componentName, durationInMicros));
            }
        }
    }


}
