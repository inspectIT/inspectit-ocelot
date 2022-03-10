package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JaegerGrpcExporterSettings {

    private boolean enabled;

    /**
     * The URL of the Jaeger server. This field is deprecated and only included for the PostConstruct test of {@link rocks.inspectit.oce.eum.server.exporters.configuration.TraceExportersConfiguration}.
     */
    @Deprecated
    private String url;

    /**
     * The URI of the Jaeger gRPC proto-buf API.
     */
    private String grpc;

    /**
     * The service name. Used in {@link rocks.inspectit.oce.eum.server.exporters.configuration.TraceExportersConfiguration}
     */
    private String serviceName;

}
