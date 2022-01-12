package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.trace.OtlpTraceExporterSettings;

import javax.validation.Valid;

/**
 * Service for {@link io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter}.
 * Can be dynamically started and stopped using the exporters.trace.otlp.enabled configuration
 */
@Component
@Slf4j
public class OtlpTraceExporterService extends DynamicallyActivatableTraceExporterService {

    @Getter
    private SpanProcessor spanProcessor;

    private OtlpGrpcSpanExporter spanExporter;


    public OtlpTraceExporterService() {
        super("exporters.tracing.otlp", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid OtlpTraceExporterSettings otlp = configuration.getExporters().getTracing().getOtlp();
        return configuration.getTracing().isEnabled() && !StringUtils.isEmpty(otlp.getUrl()) && otlp.isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            OtlpTraceExporterSettings otlp = configuration.getExporters().getTracing().getOtlp();
            log.info("Starting OTLP Trace Exporter with endpoint {}", otlp.getUrl());

            // create span exporter
            spanExporter = OtlpGrpcSpanExporter.builder().setEndpoint(otlp.getUrl()).build();

            // create span processor
            spanProcessor = BatchSpanProcessor.builder(spanExporter).build();

            // register service
            openTelemetryController.registerTraceExporterService(this);
            return true;
        } catch (Throwable t) {
            log.error("Error creating OTLP trace exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {

        log.info("Stopping OTLP trace exporter");
        try {

            // shut down span processor
            if (null != spanProcessor) {
                spanProcessor.shutdown();
                spanProcessor = null;
            }

            // unregister service
            openTelemetryController.unregisterTraceExporterService(this);
        }
        catch (Throwable t){
            log.error("Error disabling OTLP trace exporter", t);
        }
        return true;
    }
}
