package rocks.inspectit.oce.eum.server.model.config;

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
 *
 * The configuration of the inspectit-ocelot-eum-server
 */
@ConfigurationProperties("inspectit-ocelot-eum-server")
@Component
@Data
@Validated
public class Configuration {
    /**
     * List of metric definitions
     */
    @Valid
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid BeaconMetricDefinition> definitions = Collections.emptyMap();

    /**
     * Map of tags
     */
    @Valid
    private EUMTagsSettings tags;

    /**
     * The exporters settings
     */
    @Valid
    private ExportersSettings exporters;
}
