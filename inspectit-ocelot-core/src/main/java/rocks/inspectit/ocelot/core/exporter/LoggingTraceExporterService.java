package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.trace.LoggingTraceExporterSettings;
import rocks.inspectit.ocelot.core.opentelemetry.OpenTelemetryControllerImpl;

import javax.validation.Valid;

/**
 * Service for the {@link io.opentelemetry.exporter.logging.LoggingSpanExporter}
 */
@Component
@Slf4j
public class LoggingTraceExporterService extends DynamicallyActivatableTraceExporterService {

    /**
     * The {@link DynamicallyActivatableSpanExporter< LoggingSpanExporter >} for exporting the spans to the log
     */
    private DynamicallyActivatableSpanExporter<LoggingSpanExporter> spanExporter;

    @Getter
    private SpanProcessor spanProcessor;


    public LoggingTraceExporterService() {
        super("exporters.tracing.logging", "tracing.enabled");
    }

    @Override
    protected void init() {
        super.init();

        // create span exporter and span processors
        spanExporter = DynamicallyActivatableSpanExporter.createLoggingSpanExporter();
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

            // create span processors
            // SpanProcessors are also shut down when the corresponding TracerProvider is shut down. Thus, we need to create the SpanProcessors each time
            spanProcessor = SimpleSpanProcessor.create(spanExporter);

            // enable span exporter
            spanExporter.doEnable();

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
            // disable the span exporter
            if (null != spanExporter) {
                spanExporter.doDisable();
            }
            // shut down the span processor
            if (null != spanProcessor) {
                spanProcessor.shutdown();
                spanProcessor = null;
            }
            openTelemetryController.unregisterTraceExporterService(this);
            log.info("Stopping TraceLoggingSpanExporter");
            return true;
        } catch (Exception e) {
            log.error("Failed to stop TraceLoggingExporter", e);
            return false;
        }
    }
}
