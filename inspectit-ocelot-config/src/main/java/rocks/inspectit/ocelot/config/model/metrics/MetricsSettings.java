package rocks.inspectit.ocelot.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.jmx.JmxMetricsRecorderSettings;
import rocks.inspectit.ocelot.config.validation.AdditionalValidation;
import rocks.inspectit.ocelot.config.validation.AdditionalValidations;
import rocks.inspectit.ocelot.config.validation.ViolationBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AdditionalValidations
public class MetricsSettings {

    /**
     * Master switch for disabling metrics capturing and exporting.
     * If disabled the following happens:
     * - all metrics exporters are disabled
     * - all metrics recorders are disabled
     * - no measurements are collected during instrumentation, the instrumentation however is still performed
     * - no user metrics and views are created
     */
    private boolean enabled;

    /**
     * Default frequency used when polling metrics
     */
    private Duration frequency;

    @NotNull
    private Map<@NotBlank String, @NotNull @Valid MetricDefinitionSettings> definitions = Collections.emptyMap();

    /**
     * Settings for {@link rocks.inspectit.ocelot.core.metrics.system.DiskMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings disk;

    /**
     * Settings for {@link rocks.inspectit.ocelot.core.metrics.system.ClassLoaderMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings classloader;

    /**
     * Settings for {@link rocks.inspectit.ocelot.core.metrics.system.ProcessorMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings processor;

    /**
     * Settings for {@link rocks.inspectit.ocelot.core.metrics.system.ThreadMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings threads;

    /**
     * Settings for {@link rocks.inspectit.ocelot.core.metrics.system.GCMetricsRecorder}
     */
    @Valid
    private StandardMetricsSettings gc;

    /**
     * Settings for {@link rocks.inspectit.ocelot.core.metrics.system.MemoryMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings memory;

    /**
     * Settings for {@link rocks.inspectit.ocelot.core.metrics.system.JxmMetricsRecorder}
     */
    @Valid
    @NotNull
    private JmxMetricsRecorderSettings jmx;

    @AdditionalValidation
    public void noDuplicateViewNames(ViolationBuilder vios) {
        Map<String, String> viewsToMeasuresMap = new HashMap<>();
        definitions.forEach((measure, def) -> {
            val views = def.getCopyWithDefaultsPopulated(measure).getViews();
            if (!CollectionUtils.isEmpty(views)) {
                views.forEach((view, viewDef) -> {
                    if (viewDef.isEnabled()) {
                        if (viewsToMeasuresMap.containsKey(view)) {
                            vios.message("View with name '{view}' is defined for both measures '{m1}' an '{m2}'")
                                    .parameter("view", view)
                                    .parameter("m1", viewsToMeasuresMap.get(view))
                                    .parameter("m2", measure)
                                    .buildAndPublish();
                        } else {
                            viewsToMeasuresMap.put(view, measure);
                        }
                    }
                });
            }
        });
    }
}
