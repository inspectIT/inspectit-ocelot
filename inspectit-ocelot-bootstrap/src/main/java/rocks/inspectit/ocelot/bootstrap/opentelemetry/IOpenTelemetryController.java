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
     *
     * @return The name of this {@link IOpenTelemetryController}
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Registers a new {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService trace exporter service} that is used to export {@link io.opentelemetry.sdk.trace.data.SpanData} for sampled {@link io.opentelemetry.api.trace.Span}s
     * This method should ONLY be used in tests of the {@code agent} package.
     *
     * @param spanExporter The {@link SpanExporter} of the {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService trace exporter service}
     * @param serviceName  The name of the trace exporter service
     *
     * @return Whether the registration was successful
     */

    boolean registerTraceExporterService(Object spanExporter, String serviceName);

    /**
     * Sets the sampler. This method should ONY be used in tests of the {@code agent} package.
     *
     * @param sampleMode        The string value of the {@code SampleMode}
     * @param sampleProbability the sample probability
     */
    void setSampler(String sampleMode, double sampleProbability);
}
