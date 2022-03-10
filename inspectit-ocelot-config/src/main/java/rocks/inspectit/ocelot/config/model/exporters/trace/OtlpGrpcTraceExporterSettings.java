package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings for {@link rocks.inspectit.ocelot.core.exporter.OtlpGrpcTraceExporterService}
 */
@Data
@NoArgsConstructor
public class OtlpGrpcTraceExporterSettings {

    private boolean enabled;

    /***
     * The OTLP traces gRPC endpoint to connect to.
     */
    private String url;

    private String serviceName;
}
