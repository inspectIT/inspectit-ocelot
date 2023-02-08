package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.utils.EndpointUtils;

@Data
@NoArgsConstructor
public class JaegerExporterSettings {

    /**
     * Whether the exporter should be started.
     */
    private ExporterEnabledState enabled;

    @Deprecated
    /**
     * This property is deprecated since v2.0. Please use {@link #endpoint} instead.
     * The gRPC endpoint of the Jaeger server.
     */ private String grpc;

    @Deprecated
    /**
     * This property is deprecated since v2.0. Please use {@link #endpoint} instead.
     * The URL of the Jaeger server.
     */ private String url;

    /**
     * The URL endpoint of the Jaeger server.
     */
    private String endpoint;

    /**
     * The transport protocol to use.
     * Supported protocols are {@link TransportProtocol#HTTP_THRIFT} and {@link TransportProtocol#GRPC}
     */
    private TransportProtocol protocol;

    /**
     * The service name. Used in {@link rocks.inspectit.oce.eum.server.exporters.configuration.TraceExportersConfiguration}
     */
    private String serviceName;

    /**
     * Gets the URL endpoint. The endpoint is padded with 'http' to meet OTEL's requirement that the URI needs to start with 'http://' or 'https://'.
     * E.g., if you set the endpoint to 'localhost:4317', it will be returned as 'http://localhost:4317'.
     *
     * @return
     */
    public String getEndpoint() {
        return EndpointUtils.padEndpoint(endpoint);
    }

    public String getGrpc() {
        return EndpointUtils.padEndpoint(grpc);
    }

    public String getUrl() {
        return EndpointUtils.padEndpoint(url);
    }
}
