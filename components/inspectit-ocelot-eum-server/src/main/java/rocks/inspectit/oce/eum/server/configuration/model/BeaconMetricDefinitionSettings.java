package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.*;
import rocks.inspectit.oce.eum.server.arithmetic.RawExpression;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Defines the mapping of a beacon value to a OpenCensus Measure and the corresponding views.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class BeaconMetricDefinitionSettings extends MetricDefinitionSettings {

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
    public BeaconMetricDefinitionSettings(boolean enabled, @NotBlank String unit, @NotNull MeasureType type, String description,
                                          Map<@NotBlank String, @Valid @NotNull ViewDefinitionSettings> views, @NotEmpty List<BeaconRequirement> beaconRequirements,
                                          String valueExpression) {
        super(enabled, unit, type, description, views);
        this.beaconRequirements = beaconRequirements;
        this.valueExpression = valueExpression;
    }

    @Override
    public BeaconMetricDefinitionSettings getCopyWithDefaultsPopulated(String metricName) {
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

    @AssertTrue(message = "Please verify that your value expression contains a valid syntax.")
    public boolean isValidValueExpression() {
        RawExpression expression = new RawExpression(valueExpression);

        Map<String, String> dummyValueMap = expression.getFields().stream()
                .collect(Collectors.toMap(Function.identity(), x -> String.valueOf(Math.random() + 1D)));

        try {
            Number result = expression.solve(Beacon.of(dummyValueMap));

            return result != null;
        } catch (Exception exception) {
            return false;
        }
    }
}