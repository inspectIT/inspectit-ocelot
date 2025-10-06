package rocks.inspectit.ocelot.core.metrics;

import io.opentelemetry.api.metrics.*;
import io.opentelemetry.sdk.metrics.OcelotMetricUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.util.Optional;

@Component
public class InstrumentFactory {

    /**
     * Creates a new instrument. Since {@code AbstractInstrument} is package-private, we return an {@code Object}.
     * If the configured OTel Meter uses a noop-implementation, we do not create an instrument.
     *
     * @param name the instrument name
     * @param metricDefinition the metric settings
     *
     * @return the created instrument as {@code Object} or empty
     */
    public Optional<Object> createInstrument(String name, MetricDefinitionSettings metricDefinition) {
        Meter meter = OpenTelemetryUtils.getMeter();
        // Do not create noop-instruments
        if (OcelotMetricUtils.isSdkMeter(meter)) {
            switch (metricDefinition.getInstrumentType()) {
                case COUNTER:
                    return Optional.of(createCounter(name, metricDefinition, meter));
                case UP_DOWN_COUNTER:
                    return Optional.of(createUpDownCounter(name, metricDefinition,meter));
                case GAUGE:
                    return Optional.of(createGauge(name, metricDefinition, meter));
                case HISTOGRAM:
                    return Optional.of(createHistogram(name, metricDefinition, meter));
                default:
                    return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Object createCounter(String name, MetricDefinitionSettings metricDefinition, Meter meter) {
        LongCounterBuilder builder = meter
                .counterBuilder(name)
                .setDescription(metricDefinition.getDescription())
                .setUnit(metricDefinition.getUnit());

        switch (metricDefinition.getValueType()) {
            case DOUBLE:
                return builder.ofDoubles().build();
            default:
                return builder.build();
        }
    }

    private Object createUpDownCounter(String name, MetricDefinitionSettings metricDefinition, Meter meter) {
        LongUpDownCounterBuilder builder = meter
                .upDownCounterBuilder(name)
                .setDescription(metricDefinition.getDescription())
                .setUnit(metricDefinition.getUnit());

        switch (metricDefinition.getValueType()) {
            case DOUBLE:
                return builder.ofDoubles().build();
            default:
                return builder.build();
        }
    }

    private Object createGauge(String name, MetricDefinitionSettings metricDefinition, Meter meter) {
        DoubleGaugeBuilder builder = meter
                .gaugeBuilder(name)
                .setDescription(metricDefinition.getDescription())
                .setUnit(metricDefinition.getUnit());

        switch (metricDefinition.getValueType()) {
            case LONG:
                return builder.ofLongs().build();
            default:
                return builder.build();
        }
    }

    private Object createHistogram(String name, MetricDefinitionSettings metricDefinition, Meter meter) {
        DoubleHistogramBuilder builder = meter
                .histogramBuilder(name)
                .setDescription(metricDefinition.getDescription())
                .setUnit(metricDefinition.getUnit());

        switch (metricDefinition.getValueType()) {
            case LONG:
                return builder.ofLongs().build();
            default:
                return builder.build();
        }
    }
}
