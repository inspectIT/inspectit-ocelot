package rocks.inspectit.ocelot.config.model.exporters.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

/**
 * Settings for metrics exporters.
 */
@Data
@NoArgsConstructor
public class MetricsExportersSettings {

    @Valid
    private PrometheusExporterSettings prometheus;

    @Valid
    private InfluxExporterSettings influx;

    @Valid
    private LoggingMetricsExporterSettings logging;

    @Valid
    private OtlpMetricsExporterSettings otlp;
}
