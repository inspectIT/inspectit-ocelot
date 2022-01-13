package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.trace.LoggingTraceExporterSettings;

import javax.validation.Valid;

/**
 * Service for the {@link io.opentelemetry.exporter.logging.LoggingSpanExporter}
 */
@Component
@Slf4j
public class LoggingTraceExporterService extends DynamicallyActivatableTraceExporterService {

    /**
     * The {@link LoggingSpanExporter} for exporting the spans to the log
     */
    @Getter
    private LoggingSpanExporter spanExporter;

    public LoggingTraceExporterService() {
        super("exporters.tracing.logging", "tracing.enabled");
    }

    @Override
    protected void init() {
        super.init();

        // create span exporter and span processors
        spanExporter = new LoggingSpanExporter();
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid LoggingTraceExporterSettings logging = configuration.getExporters().getTracing().getLogging();
        return configuration.getTracing().isEnabled() && logging.isEnabled();

    }

    @Override
    protected boolean doEnable(InspectitConfig conf) {
        LoggingTraceExporterSettings logging = conf.getExporters().getTracing().getLogging();
        try {

            boolean success = openTelemetryController.registerTraceExporterService(this);
            if (success) {
                log.info("Starting {}", getClass().getSimpleName());
            } else {
                log.error("Failed to register {} at {}!", getClass().getSimpleName(), openTelemetryController.getClass()
                        .getSimpleName());
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to start " + getClass().getSimpleName(), e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        try {
            openTelemetryController.unregisterTraceExporterService(this);
            if (null != spanExporter) {
                spanExporter.flush();
            }
            log.info("Stopping TraceLoggingSpanExporter");
            return true;
        } catch (Exception e) {
            log.error("Failed to stop TraceLoggingExporter", e);
            return false;
        }
    }
}
