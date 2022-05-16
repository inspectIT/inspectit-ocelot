package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.trace.LoggingTraceExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

/**
 * Service for the {@link io.opentelemetry.exporter.logging.LoggingSpanExporter}
 */
@Component
@Slf4j
public class LoggingTraceExporterService extends DynamicallyActivatableService {

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
        spanExporter = LoggingSpanExporter.create();
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid LoggingTraceExporterSettings logging = configuration.getExporters().getTracing().getLogging();
        return configuration.getTracing().isEnabled() && !logging.getEnabled().isDisabled();

    }

    @Override
    protected boolean doEnable(InspectitConfig conf) {
        try {
            boolean success = openTelemetryController.registerTraceExporterService(spanExporter, getName());
            if (success) {
                log.info("Starting {}", getName());
            } else {
                log.error("Failed to register {} at the OpenTelemetry controller!", getName());
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to start " + getName(), e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        try {
            openTelemetryController.unregisterTraceExporterService(getName());
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
