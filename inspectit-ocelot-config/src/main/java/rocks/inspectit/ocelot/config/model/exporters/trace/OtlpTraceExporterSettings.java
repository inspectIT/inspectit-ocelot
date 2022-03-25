package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;

/**
 * Settings for {@link rocks.inspectit.ocelot.core.exporter.OtlpTraceExporterService}
 */
@Data
@NoArgsConstructor
public class OtlpTraceExporterSettings {

    private ExporterEnabledState enabled;

    @Deprecated
    /***
     * This property is deprecated since v2.0. Please use {@link #endpoint} instead.
     * The OTLP traces gRPC endpoint to connect to.
     */ private String url;

    /**
     * The OTLP traces endpoint to connect to.
     */
    private String endpoint;

    /**
     * The transport protocol to use.
     * Supported protocols are {@link TransportProtocol#GRPC} and {@link TransportProtocol#HTTP_PROTOBUF}
     */
    private TransportProtocol protocol;

    private String serviceName;
}
