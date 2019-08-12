package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.*;
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

    /**
     * The expression to extract a value from a beacon.
     * See {@link rocks.inspectit.oce.eum.server.arithmetic.ArithmeticExpression} for more details.
     */
    @NotEmpty
    private String valueExpression;

    /**
     * Requirements which have to be fulfilled by Beacons. Beacons which do not match all requirements will be ignored
     * by this metric definition.
     */
    @Valid
    private List<BeaconRequirement> beaconRequirements;


    @Builder(builderMethodName = "beaconMetricBuilder")
    public BeaconMetricDefinition(boolean enabled, @NotBlank String unit, @NotNull MeasureType type, String description,
                                  Map<@NotBlank String, @Valid @NotNull ViewDefinitionSettings> views, @NotEmpty List<BeaconRequirement> beaconRequirements,
                                  String valueExpression) {
        super(enabled, unit, type, description, views);
        this.beaconRequirements = beaconRequirements;
        this.valueExpression = valueExpression;
    }

    @Override
    public BeaconMetricDefinition getCopyWithDefaultsPopulated(String metricName) {
        MetricDefinitionSettings metricDefinition = super.getCopyWithDefaultsPopulated(metricName);

        return beaconMetricBuilder()
                .beaconRequirements(getBeaconRequirements())
                .valueExpression(getValueExpression())
                .description(metricDefinition.getDescription())
                .unit(metricDefinition.getUnit())
                .type(metricDefinition.getType())
                .enabled(metricDefinition.isEnabled())
                .views(metricDefinition.getViews())
                .build();
    }
}