package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JaegerExporterSettings {

    private boolean enabled;

    /**
     * The URL of the Jaeger server.
     */
    @Deprecated
    private String url;

    /**
     * The URI of the Jaeger gRPC proto-buf API.
     */
    private String grpc;

}
