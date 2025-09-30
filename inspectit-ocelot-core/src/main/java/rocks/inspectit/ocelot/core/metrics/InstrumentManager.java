package rocks.inspectit.ocelot.core.metrics;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.metrics.timewindow.worker.TimeWindowRecorder;
import rocks.inspectit.ocelot.core.opentelemetry.events.OpenTelemetryConfiguredEvent;
import rocks.inspectit.ocelot.core.opentelemetry.metrics.ViewManager;
import rocks.inspectit.ocelot.core.utils.AttributeUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central component, which is responsible for writing communication with the OpenTelemetry instruments.
 * Views will be handled via {@link ViewManager}.
 */
@Component
@Slf4j
public class InstrumentManager {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private InstrumentFactory instrumentFactory;

    @Autowired
    private TimeWindowRecorder timeWindowRecorder;

    /**
     * Created OpenTelemetry instruments referenced by their metric name.
     * Since {@code AbstractInstrument} is package-private, we store instruments as {@code Object}
     * and cast them to proper data types during recording.
     */
    private final Map<String, Object> cachedInstruments = new ConcurrentHashMap<>();

    /**
     * Caches the definitions which were used to build instruments for metrics.
     * This is used to quickly detect which metrics have changed on configuration updates.
     * This map should always use definitions with {@link MetricDefinitionSettings#getCopyWithDefaultsPopulated populated defaults}.
     */
    private final Map<String, MetricDefinitionSettings> currentMetricDefinitions = new ConcurrentHashMap<>();

    /**
     * Updates the instruments defined via {@link MetricsSettings#getDefinitions()}.
     * We should only update the instruments after the OpenTelemetry SDK has been configured.
     * Otherwise, we will create NOOP-instruments.
     */
    @EventListener
    public void updateInstruments(OpenTelemetryConfiguredEvent event) {
        MetricsSettings metricsSettings = env.getCurrentConfig().getMetrics();

        if (event.isSuccess() && metricsSettings.isEnabled()) {
            Set<String> instrumentsToRemove = processInstrumentUpdates(metricsSettings.getDefinitions());
            instrumentsToRemove.forEach(this::removeInstrument);
            log.info("Successfully updated OpenTelemetry instruments");
        }
    }

    /**
     * Processes the provided metric definitions to update {@link #cachedInstruments} and collects a set of
     * instrument names, which are no longer required.
     *
     * @param newDefinitions the new metric definitions
     *
     * @return the set of instrument names, which are no longer required
     */
    public Set<String> processInstrumentUpdates(Map<String, MetricDefinitionSettings> newDefinitions) {
        Set<String> instrumentsToRemove = new HashSet<>(cachedInstruments.keySet());

        newDefinitions.forEach((name, def) -> {
            val defWithDefaults = def.getCopyWithDefaultsPopulated(name);
            val currentDef = currentMetricDefinitions.get(name);

            boolean instrumentRequired = cachedInstruments.containsKey(name);
            if (!defWithDefaults.equals(currentDef)) {
                instrumentRequired = updateInstrument(name, defWithDefaults);
            }
            if (instrumentRequired) {
                instrumentsToRemove.remove(name);
            }

        });

        return instrumentsToRemove;
    }

    /**
     * Updates an instrument in {@link #cachedInstruments}, if the metric definition requires one.
     * Otherwise, we will remove the instrument.
     *
     * @param metricName the metric name
     * @param metricDefinition the definition for the metric
     *
     * @return true, if an instrument is required after the update
     */
    private boolean updateInstrument(String metricName, MetricDefinitionSettings metricDefinition) {
        if (requiresInstrument(metricDefinition)) {
            Object instrument = instrumentFactory.createInstrument(metricName, metricDefinition);
            cachedInstruments.put(metricName, instrument);
            currentMetricDefinitions.put(metricName, metricDefinition);
            return true;
        }
        return false;
    }

    /**
     * Removes the stored instrument and metric definition for the provided metric name.
     *
     * @param metricName the metric name
     */
    private void removeInstrument(String metricName) {
        cachedInstruments.remove(metricName);
        currentMetricDefinitions.remove(metricName);
    }

    /**
     * Records a value for the metric via OpenTelemetry {@link Meter} or/and via {@link TimeWindowRecorder}.
     *
     * @param metricName       the name of the metric
     * @param value            the value, which is going to be recorded
     *
     * @return true, if a metric was recorded
     */
    public boolean tryRecordingMetric(String metricName, Number value) {
        return tryRecordingMetric(metricName, value, Baggage.current());
    }

    /**
     * Records a value for the metric via OpenTelemetry {@link Meter} or/and via {@link TimeWindowRecorder}.
     *
     * @param metricName       the name of the metric
     * @param value            the value, which is going to be recorded
     * @param baggage          the baggage to use for recording the metric
     *
     * @return true, if a metric was recorded
     */
    public boolean tryRecordingMetric(String metricName, Number value, Baggage baggage) {
        log.trace("Trying to record metric '{}' with value '{}'", metricName, value);

        /*
            We include the whole baggage here. The attributes will be filtered via AttributesProcessor of the
            particular views, which are configured in the ViewManager.
         */
        Attributes attributes = AttributeUtils.toAttributes(baggage);

        boolean recordedInstrument = false;
        if (isInstrumentRegistered(metricName)) {
            MetricDefinitionSettings metricDefinition = currentMetricDefinitions.get(metricName);
            switch (metricDefinition.getInstrumentType()) {
                case COUNTER:
                    recordCounter(metricName, metricDefinition, value, attributes);
                    break;
                case UP_DOWN_COUNTER:
                    recordUpDownCounter(metricName, metricDefinition, value, attributes);
                    break;
                case GAUGE:
                    recordGauge(metricName, metricDefinition, value, attributes);
                    break;
                case HISTOGRAM:
                    recordHistogram(metricName, metricDefinition, value, attributes);
                    break;
                default:
                    throw new IllegalArgumentException("Tried to record unsupported instrument type: " + metricDefinition.getInstrumentType().name());
            }
            recordedInstrument = true;
        }

        boolean recordedTimeWindowMetric = timeWindowRecorder.recordMetric(metricName, value.doubleValue(), baggage);

        return recordedInstrument || recordedTimeWindowMetric;
    }

    private void recordCounter(String instrumentName, MetricDefinitionSettings metricDefinition, Number value, Attributes attributes) {
        switch (metricDefinition.getValueType()) {
            case LONG:
                LongCounter longCounter = (LongCounter) cachedInstruments.get(instrumentName);
                longCounter.add(value.longValue(), attributes);
            case DOUBLE:
                DoubleCounter doubleCounter = (DoubleCounter) cachedInstruments.get(instrumentName);
                doubleCounter.add(value.doubleValue(), attributes);
        }
    }

    private void recordUpDownCounter(String instrumentName, MetricDefinitionSettings metricDefinition, Number value, Attributes attributes) {
        switch (metricDefinition.getValueType()) {
            case LONG:
                LongUpDownCounter longCounter = (LongUpDownCounter) cachedInstruments.get(instrumentName);
                longCounter.add(value.longValue(), attributes);
            case DOUBLE:
                DoubleUpDownCounter doubleCounter = (DoubleUpDownCounter) cachedInstruments.get(instrumentName);
                doubleCounter.add(value.doubleValue(), attributes);
        }
    }

    private void recordGauge(String instrumentName, MetricDefinitionSettings metricDefinition, Number value, Attributes attributes) {
        switch (metricDefinition.getValueType()) {
            case LONG:
                LongGauge longGauge = (LongGauge) cachedInstruments.get(instrumentName);
                longGauge.set(value.longValue(), attributes);
            case DOUBLE:
                DoubleGauge doubleGauge = (DoubleGauge) cachedInstruments.get(instrumentName);
                doubleGauge.set(value.doubleValue(), attributes);
        }
    }

    private void recordHistogram(String instrumentName, MetricDefinitionSettings metricDefinition, Number value, Attributes attributes) {
        switch (metricDefinition.getValueType()) {
            case LONG:
                LongHistogram longHistogram = (LongHistogram) cachedInstruments.get(instrumentName);
                longHistogram.record(value.longValue(), attributes);
            case DOUBLE:
                DoubleHistogram doubleHistogram = (DoubleHistogram) cachedInstruments.get(instrumentName);
                doubleHistogram.record(value.doubleValue(), attributes);
        }
    }

    /**
     * Checks, if the provided metric requires an instrument. We only need an instrument, if the metric definition
     * contains a view, which uses an OpenTelemetry aggregation <b>OR</b>
     * if there are no views specified at all. Then we will use the default OpenTelemetry views,
     * which OpenTelemetry handles by itself automatically.
     * For time-window aggregations we will record metrics via {@link TimeWindowRecorder} later.
     *
     * @param metricDefinition the metric definition
     *
     * @return true, if the metric requires an instrument
     */
    private boolean requiresInstrument(MetricDefinitionSettings metricDefinition) {
        if (metricDefinition.isEnabled()) {
            boolean useDefaultView = CollectionUtils.isEmpty(metricDefinition.getViews());
            return useDefaultView || metricDefinition.getViews()
                    .values().stream()
                    .anyMatch(view -> view.getAggregation().isOpenTelemetryAggregation());
        }
       return false;
    }

    /**
     * @param metricName the name of the metric
     *
     * @return true, if an OpenTelemetry instrument was created for the metric
     */
    public boolean isInstrumentRegistered(String metricName) {
        return cachedInstruments.containsKey(metricName);
    }
}
