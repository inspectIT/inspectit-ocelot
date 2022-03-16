package rocks.inspectit.ocelot.config.model.exporters.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.time.DurationMin;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

import java.time.Duration;

/**
 * Settings for {@link rocks.inspectit.ocelot.core.exporter.OtlpGrpcMetricsExporterService}
 */
@Data
@NoArgsConstructor
public class OtlpGrpcMetricsExporterSettings {

    private ExporterEnabledState enabled;

    /***
     * The OTLP gRPC metrics endpoint to connect to.
     */
    private String url;

    /**
     * Defines how often metrics are pushed to the log.
     */
    @DurationMin(millis = 1)
    private Duration exportInterval;

}
