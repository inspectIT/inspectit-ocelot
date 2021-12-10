package rocks.inspectit.ocelot.config.model.exporters.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.time.DurationMin;

import java.time.Duration;

/**
 * Settings for the {@link io.opentelemetry.exporter.logging.LoggingMetricExporter} and
 */
@Data
@NoArgsConstructor
public class MetricLoggingExporterSettings {

    private boolean enabled;

    private String serviceName;

    /**
     * Defines how often metrics are pushed to the log.
     */
    @DurationMin(millis = 1)
    private Duration exportInterval;

}
