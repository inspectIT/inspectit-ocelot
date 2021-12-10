package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * A custom {@link MetricExporter} wrapper that can be dynamically enabled or disabled.
 */
@Slf4j
@Data
public class DynamicallyActivatableMetricExporter<T extends MetricExporter> implements MetricExporter {

    /**
     * The real {@link MetricExporter} implementation
     */
    private T exporter;

    @Setter(AccessLevel.PRIVATE)
    private boolean enabled = false;

    public DynamicallyActivatableMetricExporter(T metricExporter) {
        exporter = metricExporter;
    }

    /**
     * Creates a new {@link DynamicallyActivatableMetricExporter} with the given {@link MetricExporter} as the implemented exporter
     *
     * @param metricExporter
     *
     * @return {@link DynamicallyActivatableMetricExporter} with the given {@link MetricExporter} as the implemented exporter
     */
    public static DynamicallyActivatableMetricExporter create(MetricExporter metricExporter) {
        return new DynamicallyActivatableMetricExporter<>(metricExporter);
    }

    /**
     * Creates a new {@link DynamicallyActivatableMetricExporter} with a {@link LoggingMetricExporter} as the implemented {@link MetricExporter}
     *
     * @return {@link DynamicallyActivatableMetricExporter} with a {@link LoggingMetricExporter} as the implemented {@link MetricExporter}
     */
    public static DynamicallyActivatableMetricExporter<LoggingMetricExporter> createLoggingExporter() {
        return new DynamicallyActivatableMetricExporter<>(new LoggingMetricExporter());
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        // if enabled, call the real exporter's export method, otherwise do nothing
        return isEnabled() ? exporter.export(metrics) : CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return exporter.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return exporter.shutdown();
    }

    public boolean doEnable() {
        setEnabled(true);
        return true;
    }

    public boolean doDisable() {
        setEnabled(false);
        return true;
    }
}
