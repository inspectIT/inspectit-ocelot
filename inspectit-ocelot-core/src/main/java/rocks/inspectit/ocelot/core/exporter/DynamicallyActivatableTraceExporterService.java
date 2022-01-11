package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.sdk.trace.SpanProcessor;
import lombok.Getter;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.PrometheusExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

/**
 * Base class for trace export services that can be dynamically enabled and disabled based on the {@link InspectitConfig}.
 * This class extends {@link DynamicallyActivatableService} which handles the waiting for changes in the configuration.
 */
public abstract class DynamicallyActivatableTraceExporterService extends DynamicallyActivatableService {

    /**
     * Gets the {@link SpanProcessor} used to process {@link io.opentelemetry.api.trace.Span}
     *
     * @return The {@link SpanProcessor} used to process {@link io.opentelemetry.api.trace.Span}
     */
    public abstract SpanProcessor getSpanProcessor();

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
