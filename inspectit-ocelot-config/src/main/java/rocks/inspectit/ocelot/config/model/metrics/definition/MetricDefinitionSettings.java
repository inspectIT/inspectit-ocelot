package rocks.inspectit.ocelot.config.model.metrics.definition;

import lombok.*;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Duration;
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
     * Defines if this metric is enabled.
     * If this metric is disabled:
     * - no views for it are created
     * - no measurements for it are collected in the instrumentation. However the actions are still executed!
     */
    @Builder.Default
    private boolean enabled = true;

    @NotBlank
    private String unit;

    @Min(1)
    @Builder.Default
    private int tagValueLimit = 1;

    @NotNull
    @Builder.Default
    private MetricDefinitionSettings.MeasureType type = MeasureType.DOUBLE;

    /**
     * The description of the measure.
     * If this is null, the description is simply the name of the measure.
     */
    private String description;

    /**
     * Maps view names to their definitions for the measure defined by this {@link MetricDefinitionSettings}.
     * If this is null, a default view is created which simply exposes the last value of the metric.
     */
    @Singular
    private Map<@NotBlank String, @Valid @NotNull ViewDefinitionSettings> views;

    /**
     * Copies the settings of this object but applies the defaults, like creating a default view if no views were defined.
     * Does not provide a default time window for windowed views.
     *
     * @param metricName the name of the measure, derived form the key in {@link MetricsSettings#getDefinitions()}
     *
     * @return a copy of this view definition with the default populated
     */
    public MetricDefinitionSettings getCopyWithDefaultsPopulated(String metricName) {
        return getCopyWithDefaultsPopulated(metricName, null);
    }

    /**
     * Copies the settings of this object but applies the defaults, like creating a default view if no views were defined.
     *
     * @param metricName        the name of the measure, derived form the key in {@link MetricsSettings#getDefinitions()}
     * @param defaultTimeWindow the size of the time window to use as default for windowed metrics (e.g. quantiles)
     *
     * @return a copy of this view definition with the default populated
     */
    public MetricDefinitionSettings getCopyWithDefaultsPopulated(String metricName, Duration defaultTimeWindow) {
        val resultDescription = description == null ? metricName : description;
        val result = toBuilder().description(resultDescription).clearViews();
        if (!CollectionUtils.isEmpty(views)) {
            views.forEach((name, def) -> result.view(name, def.getCopyWithDefaultsPopulated(resultDescription, unit, defaultTimeWindow)));
        } else {
            result.view(metricName, ViewDefinitionSettings.builder()
                    .aggregation(ViewDefinitionSettings.Aggregation.LAST_VALUE)
                    .build()
                    .getCopyWithDefaultsPopulated(resultDescription, unit, defaultTimeWindow));
        }
        return result.build();
    }

}
