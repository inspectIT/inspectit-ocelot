package rocks.inspectit.ocelot.core.exporter;

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

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * Service for the {@link JaegerThriftSpanExporter}.
 * Can be dynamically started and stopped using the exporters.trace.jaeger.enabled configuration.
 */
@Component
@Slf4j
public class JaegerExporterService extends DynamicallyActivatableTraceExporterService {

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

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid JaegerExporterSettings jaeger = conf.getExporters().getTracing().getJaeger();
        if (conf.getTracing().isEnabled() && !jaeger.getEnabled().isDisabled()) {
            if (SUPPORTED_PROTOCOLS.contains(jaeger.getProtocol())) {
                if (StringUtils.hasText(jaeger.getEndpoint())) {
                    return true;
                } else if (StringUtils.hasText(jaeger.getUrl())) {
                    log.warn("You are using the deprecated property 'url'. This property will be invalid in future releases of InspectIT Ocelot, please use 'endpoint' instead.");
                    return true;
                }
            }
            if (jaeger.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                if (!SUPPORTED_PROTOCOLS.contains(jaeger.getProtocol())) {
                    log.warn("Jaeger Exporter is enabled, but wrong 'protocol' is specified. Supported values are ", Arrays.toString(SUPPORTED_PROTOCOLS.stream()
                            .map(transportProtocol -> transportProtocol.getName())
                            .toArray()));
                }
                if (!StringUtils.hasText(jaeger.getEndpoint()) && !StringUtils.hasText(jaeger.getUrl())) {
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
            String endpoint = StringUtils.hasText(settings.getEndpoint()) ? settings.getEndpoint() : settings.getUrl();
            log.info("Starting Jaeger Thrift Exporter with endpoint '{}'", endpoint);

            // create span exporter
            switch (settings.getProtocol()) {
                case GRPC: {
                    spanExporter = JaegerGrpcSpanExporter.builder().setEndpoint(endpoint).build();
                    break;
                }
                case HTTP_THRIFT: {
                    spanExporter = JaegerThriftSpanExporter.builder().setEndpoint(endpoint).build();
                    break;
                }
            }

            // register
            openTelemetryController.registerTraceExporterService(this);

            return true;
        } catch (Throwable t) {
            log.error("Error creating Jaeger exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping Jaeger Thrift Exporter");
        try {
            openTelemetryController.unregisterTraceExporterService(this);
            if (null != spanExporter) {
                spanExporter.close();
            }
        } catch (Throwable t) {
            log.error("Error disabling Jaeger Thrift Exporter", t);
        }
        return true;
    }

}
