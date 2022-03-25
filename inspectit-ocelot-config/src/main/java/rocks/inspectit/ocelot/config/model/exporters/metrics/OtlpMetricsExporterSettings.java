package rocks.inspectit.ocelot.config.model.exporters.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.time.DurationMin;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;

import java.time.Duration;

/**
 * Settings for {@link rocks.inspectit.ocelot.core.exporter.OtlpMetricsExporterService}
 */
@Data
@NoArgsConstructor
public class OtlpMetricsExporterSettings {

    private ExporterEnabledState enabled;

    @Deprecated
    /***
     * This property is deprecated since v2.0. Please use {@link #endpoint} instead.
     * The OTLP metrics endpoint to connect to.
     */ private String url;

    /**
     * The OTLP metrics endpoint to connect to.
     */
    private String endpoint;

    /**
     * The transport protocol to use.
     * Supported protocols are {@link TransportProtocol#GRPC} and {@link TransportProtocol#HTTP_PROTOBUF}
     */
    private TransportProtocol protocol;

    /**
     * Defines how often metrics are pushed to the log.
     */
    @DurationMin(millis = 1)
    private Duration exportInterval;

}
