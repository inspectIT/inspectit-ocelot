package rocks.inspectit.ocelot.bootstrap.opentelemetry;

public class NoopOpenTelemetryController implements IOpenTelemetryController {

    public static final NoopOpenTelemetryController INSTANCE = new NoopOpenTelemetryController();

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean isStopped() {
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
