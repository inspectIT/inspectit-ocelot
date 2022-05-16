package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;

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
}
