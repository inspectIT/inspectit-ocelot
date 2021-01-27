package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import rocks.inspectit.ocelot.config.model.exporters.ExportersSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

/**
 * The configuration of the EUM server.
 */
@ConfigurationProperties("inspectit-eum-server")
@Component
@Data
@Validated
public class EumServerConfiguration {

    /**
     * List of metric definitions
     */
    @Valid
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid BeaconMetricDefinitionSettings> definitions = Collections.emptyMap();

    /**
     * Map of tags
     */
    @Valid
    private EumTagsSettings tags;

    /**
     * Self Monitoring
     */
    @Valid
    private EumSelfMonitoringSettings selfMonitoring;

    /**
     * The exporters settings
     */
    @Valid
    private EumExportersSettings exporters;

    /**
     * The resource timing settings.
     */
    @Valid
    private ResourceTimingSettings resourceTiming;

}
