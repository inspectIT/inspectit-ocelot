package rocks.inspectit.ocelot.core.exporter;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.trace.JaegerExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.Arrays;
import java.util.List;

/**
 * Service for the {@link JaegerThriftSpanExporter}.
 * Can be dynamically started and stopped using the exporters.trace.jaeger.enabled configuration.
 */
@Component
@Slf4j
public class JaegerExporterService extends DynamicallyActivatableService {

    private final List<TransportProtocol> SUPPORTED_PROTOCOLS = Arrays.asList(TransportProtocol.GRPC, TransportProtocol.HTTP_THRIFT);

    @Getter
    private SpanExporter spanExporter;

    public JaegerExporterService() {
        super("exporters.tracing.jaeger", "tracing.enabled");
    }

    @Override
    protected void init() {
        super.init();
    }

    /**
     * @return returns the specified protocol to use or derives a protocol based on the deprecated fields in case no protocol has been defined.
     */
    @VisibleForTesting
    TransportProtocol getProtocol(JaegerExporterSettings jaeger) {
        boolean hasUrl = StringUtils.hasText(jaeger.getUrl());
        boolean hasGrpc = StringUtils.hasText(jaeger.getGrpc());

        // fallback if 'protocol' was not set: derive from set properties 'url'  or 'grpc'
        if (jaeger.getProtocol() == null && (hasUrl || hasGrpc)) {
            return hasUrl ? TransportProtocol.HTTP_THRIFT : TransportProtocol.GRPC;
        }
        return jaeger.getProtocol();
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        JaegerExporterSettings jaeger = conf.getExporters().getTracing().getJaeger();
        if (conf.getTracing().isEnabled() && !jaeger.getEnabled().isDisabled()) {

            boolean hasUrl = StringUtils.hasText(jaeger.getUrl());
            boolean hasGrpc = StringUtils.hasText(jaeger.getGrpc());
            boolean hasEndpoint = StringUtils.hasText(jaeger.getEndpoint());
            TransportProtocol exporterProtocol = getProtocol(jaeger);

            if (jaeger.getProtocol() != exporterProtocol) {
                log.warn("The property 'protocol' was not set. Based on the set property '{}' we assume the protocol '{}'. This fallback will be removed in future releases. Please make sure to use the property 'protocol' in future.", hasUrl ? "url" : "grpc", hasUrl ? TransportProtocol.HTTP_THRIFT.getConfigRepresentation() : TransportProtocol.GRPC.getConfigRepresentation());
            }

            if (SUPPORTED_PROTOCOLS.contains(exporterProtocol)) {
                if (hasEndpoint) {
                    return true;
                } else if (hasUrl || hasGrpc) {
                    log.warn("You are using the deprecated property '{}'. This property will be invalid in future releases of InspectIT Ocelot, please use 'endpoint' instead.", hasUrl ? "url" : "grpc");
                    return true;
                }
            }
            if (jaeger.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                if (!SUPPORTED_PROTOCOLS.contains(jaeger.getProtocol())) {
                    String supportedProtocols = Arrays.toString(SUPPORTED_PROTOCOLS.stream()
                            .map(TransportProtocol::getConfigRepresentation)
                            .toArray());
                    log.warn("Jaeger Exporter is enabled, but wrong 'protocol' is specified. Supported values are {}", supportedProtocols);
                }
                if (!hasEndpoint && !hasUrl && !hasGrpc) {
                    log.warn("Jaeger Exporter is enabled but 'endpoint' is not set.");
                }
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            JaegerExporterSettings settings = configuration.getExporters().getTracing().getJaeger();

            boolean hasUrl = StringUtils.hasText(settings.getUrl());
            boolean hasEndpoint = StringUtils.hasText(settings.getEndpoint());
            String endpoint = hasEndpoint ? settings.getEndpoint() : hasUrl ? settings.getUrl() : settings.getGrpc();

            // OTEL expects that the URI starts with 'http://' or 'https://'
            if (!endpoint.startsWith("http")) {
                endpoint = String.format("http://%s", endpoint);
            }
            switch (getProtocol(settings)) {
                case GRPC: {
                    spanExporter = JaegerGrpcSpanExporter.builder().setEndpoint(endpoint).build();
                    break;
                }
                case HTTP_THRIFT: {
                    spanExporter = JaegerThriftSpanExporter.builder().setEndpoint(endpoint).build();
                    break;
                }
            }

            boolean success = openTelemetryController.registerTraceExporterService(spanExporter, getName());
            if (success) {
                log.info("Starting Jaeger Exporter with endpoint '{}' and protocol '{}'", endpoint, getProtocol(settings));
            } else {
                log.error("Failed to register {} at the OpenTelemetry controller!", getName());
            }
            return success;
        } catch (Throwable t) {
            log.error("Error creating Jaeger Exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping Jaeger Exporter");
        try {
            openTelemetryController.unregisterTraceExporterService(getName());
            if (null != spanExporter) {
                spanExporter.close();
            }
        } catch (Throwable t) {
            log.error("Error disabling Jaeger Exporter", t);
        }
        return true;
    }

}
