package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import lombok.Singular;
import org.springframework.validation.annotation.Validated;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.selfmonitoring.SelfMonitoringSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@Validated
public class EumSelfMonitoringSettings extends SelfMonitoringSettings {

    @Singular
    private Map<@NotBlank String, @Valid @NotNull MetricDefinitionSettings> metrics;
}
