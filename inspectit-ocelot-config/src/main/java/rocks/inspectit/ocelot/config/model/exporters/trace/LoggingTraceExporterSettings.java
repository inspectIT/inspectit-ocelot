package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

/**
 * Settings for the {@link io.opentelemetry.exporter.logging.LoggingSpanExporter}
 */
@Data
@NoArgsConstructor
public class LoggingTraceExporterSettings {

    private ExporterEnabledState enabled;

    private String serviceName;

}
