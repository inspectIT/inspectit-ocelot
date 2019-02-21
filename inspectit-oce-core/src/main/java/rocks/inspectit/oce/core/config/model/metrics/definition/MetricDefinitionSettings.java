package rocks.inspectit.oce.core.config.model.metrics.definition;

import lombok.*;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Defines an OpenCensus measure in combination with one or multiple views
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MetricDefinitionSettings {

    public enum MeasureType {
        LONG, DOUBLE
    }

    /**
     * The name of the measure.
     * If this is null, the name defaults the key in {@link MetricsSettings#getDefinitions()}.
     * map.
     * This property can be used if the name contains ${}placeholders.
     */
    private String name;

    /**
     * Defines if this metric is enabled.
     * If this metric is disabled:
     * - no views for it are created
     * - no measurements for it are collected in the instrumentation. However the data providers are still executed!
     */
    @Builder.Default
    private boolean enabled = true;

    @NotNull
    private String unit;

    @NotNull
    @Builder.Default
    private MetricDefinitionSettings.MeasureType type = MeasureType.DOUBLE;

    /**
     * The description of the measure.
     * If this is null, the description is simply the name of the measure.
     */
    private String description;

    /**
     * Maps view names to their defintions for the measure defined by this {@link MetricDefinitionSettings}.
     * If this is null, a default view is created which simply exposes the last value of the metric.
     */
    @Singular
    private Map<@NotBlank String, @Valid @NotNull ViewDefinitionSettings> views;

    /**
     * copies tehse settings but applies the defaults, like vreating a view.
     *
     * @param defaultName the default name of the measure, derived form the key in {@link MetricsSettings#getDefinitions()}
     * @return a copy of this view definition with the default populated
     */
    public MetricDefinitionSettings getCopyWithDefaultsPopulated(String defaultName) {
        val resultName = name == null ? defaultName : name;
        val resultDescription = description == null ? resultName : description;
        val result = toBuilder()
                .description(resultDescription)
                .name(resultName)
                .clearViews();
        if (views != null && !views.isEmpty()) {
            views.forEach((name, def) ->
                    result.view(name, def.getCopyWithDefaultsPopulated(name, resultDescription, unit)));
        } else {
            result.view(resultName, ViewDefinitionSettings.builder()
                    .aggregation(ViewDefinitionSettings.Aggregation.LAST_VALUE)
                    .build()
                    .getCopyWithDefaultsPopulated(resultName, resultDescription, unit));
        }
        return result.build();
    }

}
