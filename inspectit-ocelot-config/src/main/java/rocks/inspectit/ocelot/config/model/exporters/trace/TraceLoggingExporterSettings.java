package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings for the {@link io.opentelemetry.exporter.logging.LoggingMetricExporter}
 */
@Data
@NoArgsConstructor
public class TraceLoggingExporterSettings {

    private boolean enabled;

    private String serviceName;

}
