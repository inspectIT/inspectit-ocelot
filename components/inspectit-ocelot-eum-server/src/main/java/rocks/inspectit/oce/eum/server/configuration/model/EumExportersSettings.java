package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import org.springframework.validation.annotation.Validated;
import rocks.inspectit.ocelot.config.model.exporters.metrics.MetricsExportersSettings;

import javax.validation.Valid;

/**
 * Extended exporter settings.
 */
@Data
@Validated
public class EumExportersSettings {

    /**
     * Exporter settings for beacon exporters.
     */
    private BeaconExporterSettings beacons;

    /**
     * Exporter settings for metric exporters.
     */
    @Valid
    private MetricsExportersSettings metrics;
    
    /**
     * Exporter settings for trace exporters.
     */
    @Valid
    private EumTraceExportersSettings tracing;
}
