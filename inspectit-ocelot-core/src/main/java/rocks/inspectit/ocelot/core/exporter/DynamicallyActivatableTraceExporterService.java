package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.PrometheusExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

/**
 * Base class for trace export services that can be dynamically enabled and disabled based on the {@link InspectitConfig}.
 * This class extends {@link DynamicallyActivatableService} which handles the waiting for changes in the configuration.
 */
public abstract class DynamicallyActivatableTraceExporterService extends DynamicallyActivatableService {

    /**
     * Gets the {@link SpanExporter} of this service to export {@link io.opentelemetry.sdk.trace.data.SpanData} to
     *
     * @return The {@link SpanExporter} of this service to export {@link io.opentelemetry.sdk.trace.data.SpanData} to
     */
    public abstract SpanExporter getSpanExporter();

    /**
     * Constructor.
     *
     * @param configDependencies The list of configuration properties in camelCase this service depends on.
     *                           For example "exporters.metrics.prometheus" specifies a dependency
     *                           to {@link PrometheusExporterSettings}
     *                           and all its children.
     */
    public DynamicallyActivatableTraceExporterService(String... configDependencies) {
        super(configDependencies);
    }

}
