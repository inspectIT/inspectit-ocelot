package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.trace.OtlpTraceExporterSettings;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * Service for {@link io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter}/{@link io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter}.
 * Can be dynamically started and stopped using the exporters.trace.otlp.enabled configuration
 */
@Component
@Slf4j
public class OtlpTraceExporterService extends DynamicallyActivatableTraceExporterService {

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
            if (SUPPORTED_PROTOCOLS.contains(otlp.getProtocol())) {
                if (StringUtils.hasText(otlp.getEndpoint())) {
                    return true;
                } else if (StringUtils.hasText(otlp.getUrl())) {
                    log.warn("You are using the deprecated property 'url'. This property will be invalid in future releases of InspectIT Ocelot, please use 'endpoint' instead.");
                    return true;
                }
            }
            if (otlp.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                if (!SUPPORTED_PROTOCOLS.contains(otlp.getProtocol())) {
                    log.warn("OTLP Trace Exporter is enabled, but wrong 'protocol' is specified. Supported values are ", Arrays.toString(SUPPORTED_PROTOCOLS.stream()
                            .map(transportProtocol -> transportProtocol.getName())
                            .toArray()));
                }
                if (!StringUtils.hasText(otlp.getEndpoint()) && !StringUtils.hasText(otlp.getUrl())) {
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
            String endpoint = StringUtils.hasText(otlp.getEndpoint()) ? otlp.getEndpoint() : otlp.getUrl();
            log.info("Starting OTLP Trace Exporter with endpoint {}", endpoint);

            // create span exporter
            switch (otlp.getProtocol()) {
                case GRPC: {
                    spanExporter = OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build();
                    break;
                }
                case HTTP_PROTOBUF: {
                    spanExporter = OtlpHttpSpanExporter.builder().setEndpoint(endpoint).build();
                    break;
                }
            }

            // register service
            openTelemetryController.registerTraceExporterService(this);
            return true;
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
            openTelemetryController.unregisterTraceExporterService(this);
            if (null != spanExporter) {
                spanExporter.close();
            }
        } catch (Throwable t) {
            log.error("Error disabling OTLP Trace Exporter", t);
        }
        return true;
    }
}
