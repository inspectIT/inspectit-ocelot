package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.trace.JaegerExporterSettings;

import javax.validation.Valid;

/**
 * Service for the {@link JaegerGrpcSpanExporter} exporter.
 * Can be dynamically started and stopped using the exporters.trace.jaeger-grpc.enabled configuration.
 */
@Component
@Slf4j
public class JaegerGrpcExporterService extends DynamicallyActivatableTraceExporterService {

    @Getter
    private JaegerGrpcSpanExporter spanExporter;

    public JaegerGrpcExporterService() {
        super("exporters.tracing.jaegerGrpc", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid JaegerExporterSettings jaeger = conf.getExporters().getTracing().getJaegerGrpc();
        if (conf.getTracing().isEnabled() && jaeger.isEnabled()) {
            if (!StringUtils.isEmpty(jaeger.getGrpc())) {
                return true;
            } else if (StringUtils.isNotEmpty(jaeger.getUrl())) {
                // print warning if user used wrong setup
                log.warn("In order to use Jaeger gRPC span exporter, please specify the gRPC URL endpoint property instead of the HTTP URL.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            JaegerExporterSettings settings = configuration.getExporters().getTracing().getJaegerGrpc();
            log.info("Starting Jaeger gRPC Exporter with grpc '{}'", settings.getGrpc());

            spanExporter = JaegerGrpcSpanExporter.builder()
                    .setEndpoint(settings.getGrpc() == null ? settings.getUrl() : settings.getGrpc())
                    .build();

            // register
            openTelemetryController.registerTraceExporterService(this);

            return true;
        } catch (Throwable t) {
            log.error("Error creating Jaeger gRPC Exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping Jaeger gRPC Exporter");
        try {
            openTelemetryController.unregisterTraceExporterService(this);
            if (null != spanExporter) {
                spanExporter.close();
            }
        } catch (Throwable t) {
            log.error("Error disabling Jaeger gRPC Exporter", t);
        }
        return true;
    }
}
