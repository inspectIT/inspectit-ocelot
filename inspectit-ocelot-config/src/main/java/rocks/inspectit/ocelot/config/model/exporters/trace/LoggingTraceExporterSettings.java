package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings for the {@link io.opentelemetry.exporter.logging.LoggingSpanExporter}
 */
@Data
@NoArgsConstructor
public class LoggingTraceExporterSettings {

    private boolean enabled;

    private String serviceName;

}
