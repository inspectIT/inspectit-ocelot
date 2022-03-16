package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.trace.OtlpGrpcTraceExporterSettings;

import javax.validation.Valid;

/**
 * Service for {@link io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter}.
 * Can be dynamically started and stopped using the exporters.trace.otlp-grpc.enabled configuration
 */
@Component
@Slf4j
public class OtlpGrpcTraceExporterService extends DynamicallyActivatableTraceExporterService {

    @Getter
    private SpanExporter spanExporter;

    public OtlpGrpcTraceExporterService() {
        super("exporters.tracing.otlpGrpc", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid OtlpGrpcTraceExporterSettings otlp = configuration.getExporters().getTracing().getOtlpGrpc();
        if (configuration.getTracing().isEnabled() && !otlp.getEnabled().isDisabled()) {
            if (org.springframework.util.StringUtils.hasText(otlp.getUrl())) {
                return true;
            } else if (otlp.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                log.warn("OTLP gRPC Trace Exporter is enabled but 'url' is not set.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            OtlpGrpcTraceExporterSettings otlp = configuration.getExporters().getTracing().getOtlpGrpc();
            log.info("Starting OTLP Trace Exporter with endpoint {}", otlp.getUrl());

            // create span exporter
            spanExporter = OtlpGrpcSpanExporter.builder().setEndpoint(otlp.getUrl()).build();

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
            // unregister service
            openTelemetryController.unregisterTraceExporterService(this);
            if (null != spanExporter) {
                spanExporter.close();
            }
        } catch (Throwable t) {
            log.error("Error disabling OTLP trace exporter", t);
        }
        return true;
    }
}
