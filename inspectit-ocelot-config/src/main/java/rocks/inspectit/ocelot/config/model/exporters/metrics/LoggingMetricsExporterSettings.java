package rocks.inspectit.ocelot.config.model.exporters.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.time.DurationMin;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

import java.time.Duration;

/**
 * Settings for the {@link io.opentelemetry.exporter.logging.LoggingMetricExporter}
 */
@Data
@NoArgsConstructor
public class LoggingMetricsExporterSettings {

    private ExporterEnabledState enabled;

    /**
     * Defines how often metrics are pushed to the log.
     */
    @DurationMin(millis = 1)
    private Duration exportInterval;

}
