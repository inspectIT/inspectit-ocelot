package rocks.inspectit.ocelot.config.model.metrics.definition;

import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.InstrumentValueType;
import lombok.*;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Defines an OpenTelemetry metric in combination with views
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MetricDefinitionSettings {

    /**
     * Defines if this metric is enabled.
     * If this metric is disabled:
     * - no views for it are created
     * - no metrics for it are collected in the instrumentation. However, the actions are still executed!
     */
    @Builder.Default
    private boolean enabled = true;

    @NotBlank
    private String unit;

    @Builder.Default
    private int maxValuesPerAttribute = -1;

    @NotNull
    @Builder.Default
    private InstrumentType instrumentType = InstrumentType.GAUGE;

    @NotNull
    @Builder.Default
    private InstrumentValueType valueType = InstrumentValueType.LONG;

    /**
     * The description of the metric.
     * If this is null, the description is simply the name of the metric.
     */
    private String description;

    /**
     * Maps view names to their definitions for the metric defined by this {@link MetricDefinitionSettings}.
     * If this map is null, OpenTelemetry will create a default view automatically.
     */
    @Singular
    private Map<@NotBlank String, @Valid @NotNull ViewDefinitionSettings> views;


    /**
     * Copies the settings of this object but applies the defaults, like creating a default view if no views were defined.
     *
     * @param metricName  the name of the metric, derived form the key in {@link MetricsSettings#getDefinitions()}
     *
     * @return a copy of this definition with defaults populated
     */
    public MetricDefinitionSettings getCopyWithDefaultsPopulated(String metricName) {
        val resultDescription = description == null ? metricName : description;
        val result = toBuilder().description(resultDescription).clearViews();

        if (!CollectionUtils.isEmpty(views)) {
            views.forEach((name, def) -> result.view(name, def.getCopyWithDefaultsPopulated(resultDescription, unit)));
        }

        return result.build();
    }
}
