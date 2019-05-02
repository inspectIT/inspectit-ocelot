package rocks.inspectit.ocelot.core.config.model.exporters.metrics;

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
    private OpenCensusAgentMetricsExporterSettings openCensusAgent;
}
