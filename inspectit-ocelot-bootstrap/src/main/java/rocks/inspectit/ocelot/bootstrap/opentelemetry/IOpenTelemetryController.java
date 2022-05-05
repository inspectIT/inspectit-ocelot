package rocks.inspectit.ocelot.bootstrap.opentelemetry;

/**
 * Controller interface for the OpenTelemetryController. Its implementation is {@link rocks.inspectit.ocelot.core.opentelemetry.OpenTelemetryControllerImpl}
 */
public interface IOpenTelemetryController {

    /**
     * Gets whether the {@link IOpenTelemetryController} is configured and active.
     *
     * @return Whether the {@link IOpenTelemetryController} is configured and active.
     */
    boolean isActive();

    /**
     * Gets whether the {@link IOpenTelemetryController} has been shut down. Once shut down, it cannot be restarted
     *
     * @return Whether the {@link IOpenTelemetryController} has been shut down.
     */
    boolean isShutdown();

    /**
     * Starts the {@link IOpenTelemetryController}
     *
     * @return Whether the {@link IOpenTelemetryController} was successfully started
     */
    boolean start();

    /**
     * Flushes all pending spans and metrics and waits for it to complete.
     */
    void flush();

    /**
     * Shuts down the {@link IOpenTelemetryController}.
     * The shutdown is final, i.e., once this {@link IOpenTelemetryController} is shutdown, it cannot be re-enabled!
     */
    void shutdown();

    /**
     * Notifies the {@link IOpenTelemetryController} that something in the {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings} or in the {@link rocks.inspectit.ocelot.config.model.exporters.trace.TraceExportersSettings} changed
     */
    void notifyTracingSettingsChanged();

    /**
     * Notifies the {@link IOpenTelemetryController} that something in the {@link rocks.inspectit.ocelot.config.model.metrics.MetricsSettings} or in the {@link rocks.inspectit.ocelot.config.model.exporters.metrics.MetricsExportersSettings} changed
     */
    void notifyMetricsSettingsChanged();

    /**
     * Returns the name of this {@link IOpenTelemetryController}
     * @return The name of this {@link IOpenTelemetryController}
     */
    default String getName(){
        return getClass().getSimpleName();
    }
}
