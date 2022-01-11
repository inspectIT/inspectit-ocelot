package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings for {@link rocks.inspectit.ocelot.core.exporter.OtlpTraceExporterService}
 */
@Data
@NoArgsConstructor
public class OtlpTraceExporterSettings {

    private boolean enabled;

    /***
     * The OTLP traces endpoint to connect to
     */
    private String url;

    private String serviceName;
}
