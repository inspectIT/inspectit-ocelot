package rocks.inspectit.ocelot.config.model.exporters;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.tags.TagsExporterSettings;
import rocks.inspectit.ocelot.config.model.exporters.trace.TraceExportersSettings;
import rocks.inspectit.ocelot.config.model.exporters.metrics.MetricsExportersSettings;

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

    @Valid
    private TagsExporterSettings tags;

}
