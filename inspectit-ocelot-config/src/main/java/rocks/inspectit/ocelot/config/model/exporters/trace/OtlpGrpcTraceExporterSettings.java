package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

/**
 * Settings for {@link rocks.inspectit.ocelot.core.exporter.OtlpGrpcTraceExporterService}
 */
@Data
@NoArgsConstructor
public class OtlpGrpcTraceExporterSettings {

    private ExporterEnabledState enabled;

    /***
     * The OTLP traces gRPC endpoint to connect to.
     */
    private String url;

    private String serviceName;
}
