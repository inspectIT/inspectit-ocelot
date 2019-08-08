package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.*;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Defines the mapping of a beacon value to a OpenCensus Measure and the corresponding views.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class BeaconMetricDefinition extends MetricDefinitionSettings {

    @NotBlank
    private String beaconField;

    @Builder(builderMethodName = "beaconMetricBuilder")
    public BeaconMetricDefinition(boolean enabled, @NotBlank String unit, @NotNull MeasureType type, String description,
                                  Map<@NotBlank String, @Valid @NotNull ViewDefinitionSettings> views, @NotBlank String beaconField) {
        super(enabled, unit, type, description, views);
        this.beaconField = beaconField;
    }

    @Override
    public BeaconMetricDefinition getCopyWithDefaultsPopulated(String metricName) {
        val metricDefinition = super.getCopyWithDefaultsPopulated(metricName);
        val beaconMetricDefinition = beaconMetricBuilder()
                .beaconField(getBeaconField())
                .description(metricDefinition.getDescription())
                .unit(metricDefinition.getUnit())
                .type(metricDefinition.getType())
                .enabled(metricDefinition.isEnabled())
                .views(metricDefinition.getViews()).build();
        return beaconMetricDefinition;
    }
}