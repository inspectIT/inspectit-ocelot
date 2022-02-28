package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledSetting;

@Data
@NoArgsConstructor
public class JaegerExporterSettings {

    private ExporterEnabledSetting enabled;

    /**
     * The URL of the Jaeger server.
     */
    @Deprecated
    private String url;

    /**
     * The URI of the Jaeger gRPC proto-buf API.
     */
    private String grpc;

    /**
     * The service name under which traces are published, defaults to inspectit.service-name;
     */
    private String serviceName;

}
