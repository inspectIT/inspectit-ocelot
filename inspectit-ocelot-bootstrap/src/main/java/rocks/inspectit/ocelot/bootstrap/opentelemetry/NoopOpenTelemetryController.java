package rocks.inspectit.ocelot.bootstrap.opentelemetry;

/**
 * No-operations implementation of the {@link IOpenTelemetryController}.
 */
public class NoopOpenTelemetryController implements IOpenTelemetryController {

    public static final NoopOpenTelemetryController INSTANCE = new NoopOpenTelemetryController();

    @Override
    public boolean isActive() {
        return false;
    }
    
    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void flush() {

    }

    @Override
    public void notifyTracingSettingsChanged() {

    }

    @Override
    public void notifyMetricsSettingsChanged() {

    }
}
