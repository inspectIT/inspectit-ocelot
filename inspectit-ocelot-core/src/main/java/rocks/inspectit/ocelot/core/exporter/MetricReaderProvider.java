package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;

public interface MetricReaderProvider {

    /**
     * Gets a new {@link MetricReader} for this service.
     * It is important that this method returns a <strong>new</strong> {@link MetricReader},
     * as when the previously used {@link MetricReader} is shut down during {@link SdkMeterProvider#shutdown()},
     * it cannot be re-enabled.
     *
     * @return A new {@link MetricReader}
     */
    MetricReader getNewMetricReader();
}
