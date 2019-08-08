package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.*;
import rocks.inspectit.oce.eum.server.beacon.extractor.BeaconFieldExtractor;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Defines the mapping of a beacon value to a OpenCensus Measure and the corresponding views.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class BeaconMetricDefinition extends MetricDefinitionSettings {

    @NotNull
    private BeaconFieldExtractor extractor;

    @NotEmpty
    private List<String> beaconFields;

    @Builder(builderMethodName = "beaconMetricBuilder")
    public BeaconMetricDefinition(boolean enabled, @NotBlank String unit, @NotNull MeasureType type, String description,
                                  Map<@NotBlank String, @Valid @NotNull ViewDefinitionSettings> views, @NotEmpty List<String> beaconFields,
                                  @NotNull BeaconFieldExtractor extractor) {
        super(enabled, unit, type, description, views);
        this.beaconFields = beaconFields;
        this.extractor = extractor;
    }

    @Override
    public BeaconMetricDefinition getCopyWithDefaultsPopulated(String metricName) {
        MetricDefinitionSettings metricDefinition = super.getCopyWithDefaultsPopulated(metricName);

        return beaconMetricBuilder()
                .beaconFields(getBeaconFields())
                .extractor(getExtractor())
                .description(metricDefinition.getDescription())
                .unit(metricDefinition.getUnit())
                .type(metricDefinition.getType())
                .enabled(metricDefinition.isEnabled())
                .views(metricDefinition.getViews())
                .build();
    }
}