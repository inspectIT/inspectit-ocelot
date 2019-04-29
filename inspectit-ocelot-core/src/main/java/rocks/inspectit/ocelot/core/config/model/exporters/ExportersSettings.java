package rocks.inspectit.ocelot.core.config.model.exporters;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.core.config.model.exporters.metrics.MetricsExportersSettings;
import rocks.inspectit.ocelot.core.config.model.exporters.trace.TraceExportersSettings;

import javax.validation.Valid;

/**
 * Settings for metrics and trace exporters of OpenCensus.
 */
@Data
@NoArgsConstructor
public class ExportersSettings {

    @Valid
    private MetricsExportersSettings metrics;

    @Valid
    private TraceExportersSettings tracing;

}
