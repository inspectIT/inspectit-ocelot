package rocks.inspectit.ocelot.config.model.exporters.metrics;

import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.time.DurationMin;
import rocks.inspectit.ocelot.config.model.exporters.CompressionMethod;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.utils.EndpointUtils;

import java.time.Duration;
import java.util.Map;

/**
 * Settings for {@link rocks.inspectit.ocelot.core.exporter.OtlpMetricsExporterService}
 */
@Data
@NoArgsConstructor
public class OtlpMetricsExporterSettings {

    private ExporterEnabledState enabled;

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

    /**
     * The time period over which metrics should be aggregated.
     * Valid values are CUMULATIVE and DELTA
     */
    private AggregationTemporality preferredTemporality;

    /**
     * Key-value pairs to be used as headers associated with gRPC or HTTP requests.
     */
    private Map<String, String> headers;

    /**
     * The compression method.
     */
    private CompressionMethod compression;

    /**
     * Maximum time the OTLP exporter will wait for each batch export.
     */
    @DurationMin(millis = 1)
    private Duration timeout;

    /**
     * Gets the OTLP metrics endpoint. The endpoint is padded with 'http' to meet OTEL's requirement that the URI needs to start with 'http://' or 'https://'.
     * E.g., if you set the endpoint to 'localhost:4317', it will be returned as 'http://localhost:4317'.
     *
     * @return
     */
    public String getEndpoint() {
        return EndpointUtils.padEndpoint(endpoint);
    }
}
