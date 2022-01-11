package rocks.inspectit.ocelot.bootstrap.opentelemetry;

/**
 * Controller interface for the OpenTelemetryController. Its implementation is {@link rocks.inspectit.ocelot.core.opentelemetry.OpenTelemetryControllerImpl}
 */
public interface IOpenTelemetryController {

    /**
     * Shuts down the {@link IOpenTelemetryController}
     */
    void shutdown();

    /**
     * Starts the {@link IOpenTelemetryController}
     *
     * @return Whether the {@link IOpenTelemetryController} was successfuly started
     */
    boolean start();

    /**
     * Gets whether the {@link IOpenTelemetryController} is configured
     *
     * @return Whether the {@link IOpenTelemetryController} is configured
     */
    boolean isConfigured();

    /**
     * Gets whether the {@link IOpenTelemetryController} is enabled
     *
     * @return Whether the {@link IOpenTelemetryController} is enabled
     */
    boolean isEnabled();

    /**
     * Notifies the {@link IOpenTelemetryController} that something in the {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings} changed
     */
    void notifyTracingSettingsChanged();

    /**
     * Notifies the {@link IOpenTelemetryController} that something in the {@link rocks.inspectit.ocelot.config.model.metrics.MetricsSettings} changed
     */
    void notifyMetricsSettingsChanged();
}
