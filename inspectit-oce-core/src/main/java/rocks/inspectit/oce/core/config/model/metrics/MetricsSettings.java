package rocks.inspectit.oce.core.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class MetricsSettings {

    /**
     * Master switch for disabling metrics capturing and exporting.
     * If disabled the following happens:
     * - all metrics exporters are disabled
     * - all metrics recorders are disabled
     */
    private boolean enabled;

    /**
     * Settings for {@link rocks.inspectit.oce.core.metrics.DiskMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings disk;

    /**
     * Settings for {@link rocks.inspectit.oce.core.metrics.ClassLoaderMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings classloader;

    /**
     * Settings for {@link rocks.inspectit.oce.core.metrics.ProcessorMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings processor;

    /**
     * Settings for {@link rocks.inspectit.oce.core.metrics.ThreadMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings threads;

    /**
     * Settings for {@link rocks.inspectit.oce.core.metrics.GCMetricsRecorder}
     */
    @Valid
    private StandardMetricsSettings gc;

    /**
     * Settings for {@link rocks.inspectit.oce.core.metrics.MemoryMetricsRecorder}
     */
    @Valid
    private StandardPollingMetricsRecorderSettings memory;
}
