package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

/**
 * Beacon exporter settings.
 */
@Data
@Validated
public class BeaconExporterSettings {

    /**
     * Settings for exporting beacons via HTTP.
     */
    @Valid
    private BeaconHttpExporterSettings http;

}
