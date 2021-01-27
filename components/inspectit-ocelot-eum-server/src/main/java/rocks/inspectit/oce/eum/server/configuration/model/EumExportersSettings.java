package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;
import rocks.inspectit.ocelot.config.model.exporters.ExportersSettings;

/**
 * Extended exporter settings.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Validated
public class EumExportersSettings extends ExportersSettings {

    /**
     * Exporter settings for beacon exporters.
     */
    private BeaconExporterSettings beacons;

}
