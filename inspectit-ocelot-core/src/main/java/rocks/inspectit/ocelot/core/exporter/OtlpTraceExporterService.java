package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.trace.OtlpTraceExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Service for {@link io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter}/{@link io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter}.
 * Can be dynamically started and stopped using the exporters.trace.otlp.enabled configuration
 */
@Component
@Slf4j
public class OtlpTraceExporterService extends DynamicallyActivatableService {

    private final List<TransportProtocol> SUPPORTED_PROTOCOLS = Arrays.asList(TransportProtocol.GRPC, TransportProtocol.HTTP_PROTOBUF);

    @Getter
    private SpanExporter spanExporter;

    public OtlpTraceExporterService() {
        super("exporters.tracing.otlp", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid OtlpTraceExporterSettings otlp = configuration.getExporters().getTracing().getOtlp();
        if (configuration.getTracing().isEnabled() && !otlp.getEnabled().isDisabled()) {
            if (SUPPORTED_PROTOCOLS.contains(otlp.getProtocol()) && StringUtils.hasText(otlp.getEndpoint())) {
                return true;
            }
            if (otlp.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                if (!SUPPORTED_PROTOCOLS.contains(otlp.getProtocol())) {
                    log.warn("OTLP Trace Exporter is enabled, but wrong 'protocol' is specified. Supported values are ", Arrays.toString(SUPPORTED_PROTOCOLS.stream()
                            .map(transportProtocol -> transportProtocol.getConfigRepresentation())
                            .toArray()));
                }
                if (!StringUtils.hasText(otlp.getEndpoint())) {
                    log.warn("OTLP Trace Exporter is enabled but 'endpoint' is not set.");
                }
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            OtlpTraceExporterSettings otlp = configuration.getExporters().getTracing().getOtlp();

            switch (otlp.getProtocol()) {
                case GRPC: {
                    OtlpGrpcSpanExporterBuilder otlpGrpcSpanExporterBuilder =OtlpGrpcSpanExporter.builder().setEndpoint(otlp.getEndpoint())
                            .setCompression(otlp.getCompression().toString())
                            .setTimeout(otlp.getTimeout());
                    if(otlp.getHeaders() != null){
                        for (Map.Entry<String, String> headerEntry : otlp.getHeaders().entrySet()) {
                            otlpGrpcSpanExporterBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
                        }
                    }
                    spanExporter = otlpGrpcSpanExporterBuilder.build();
                    break;
                }
                case HTTP_PROTOBUF: {
                    OtlpHttpSpanExporterBuilder otlpHttpSpanExporterBuilder =OtlpHttpSpanExporter.builder().setEndpoint(otlp.getEndpoint()).setCompression(otlp.getCompression().toString())
                            .setTimeout(otlp.getTimeout());
                    if(otlp.getHeaders() != null){
                        for (Map.Entry<String, String> headerEntry : otlp.getHeaders().entrySet()) {
                            otlpHttpSpanExporterBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
                        }
                    }
                    spanExporter = otlpHttpSpanExporterBuilder.build();
                    break;
                }
            }

            boolean success = openTelemetryController.registerTraceExporterService(spanExporter, getName());
            if (success) {
                log.info("Starting OTLP Trace Exporter with endpoint {}", otlp.getEndpoint());
            } else {
                log.error("Failed to register {} at the OpenTelemetry controller!", getName());
            }
            return success;
        } catch (Throwable t) {
            log.error("Error creating OTLP Trace Exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {

        log.info("Stopping OTLP Trace Exporter");
        try {
            // unregister service
            openTelemetryController.unregisterTraceExporterService(getName());
            if (null != spanExporter) {
                spanExporter.close();
            }
        } catch (Throwable t) {
            log.error("Error disabling OTLP Trace Exporter", t);
        }
        return true;
    }
}
