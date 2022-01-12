package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.PrometheusExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

/**
 * Base class for metrics export services that can be dynamically enabled and disabled based on the {@link InspectitConfig}.
 * This class extends {@link DynamicallyActivatableService} which handles the waiting for changes in the configuration.
 */
public abstract class DynamicallyActivatableMetricsExporterService extends DynamicallyActivatableService {

    /**
     * Gets a new {@link MetricReaderFactory} for this service.
     *
     * @return A new {@link MetricReaderFactory}
     */
    public abstract MetricReaderFactory getNewMetricReaderFactory();

    /**
     * Constructor.
     *
     * @param configDependencies The list of configuration properties in camelCase this service depends on.
     *                           For example "exporters.metrics.prometheus" specifies a dependency
     *                           to {@link PrometheusExporterSettings}
     *                           and all its children.
     */
    public DynamicallyActivatableMetricsExporterService(String... configDependencies) {
        super(configDependencies);
    }

}
