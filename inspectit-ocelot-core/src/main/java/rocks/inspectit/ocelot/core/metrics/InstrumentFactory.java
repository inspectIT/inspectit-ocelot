package rocks.inspectit.ocelot.core.metrics;

import io.opentelemetry.api.metrics.*;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

@Component
public class InstrumentFactory {

    /**
     * Creates a new instrument. Since {@code AbstractInstrument} is package-private, we return an {@code Object}
     *
     * @param name the instrument name
     * @param metricDefinition the metric settings
     *
     * @return the created instrument as {@code Object}
     */
    public Object createInstrument(String name, MetricDefinitionSettings metricDefinition) {
        Meter meter = OpenTelemetryUtils.getMeter();
        switch (metricDefinition.getInstrumentType()) {
            case COUNTER:
                return createCounter(name, metricDefinition, meter);
            case UP_DOWN_COUNTER:
                return createUpDownCounter(name, metricDefinition,meter);
            case GAUGE:
                return createGauge(name, metricDefinition, meter);
            case HISTOGRAM:
                return createHistogram(name, metricDefinition, meter);
            default:
                throw new IllegalArgumentException("Tried to create unsupported instrument type:" + metricDefinition.getInstrumentType().name());
        }
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
