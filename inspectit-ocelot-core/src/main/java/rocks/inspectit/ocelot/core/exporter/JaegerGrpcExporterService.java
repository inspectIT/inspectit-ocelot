package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.trace.JaegerGrpcExporterSettings;

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
        @Valid JaegerGrpcExporterSettings jaeger = conf.getExporters().getTracing().getJaegerGrpc();
        if (conf.getTracing().isEnabled() && !jaeger.getEnabled().isDisabled()) {

            if (StringUtils.hasText(jaeger.getGrpc())) {
                return true;
            } else if (StringUtils.hasText(jaeger.getUrl())) {
                // print warning if user used wrong setup
                log.warn("In order to use Jaeger gRPC span exporter, please specify the 'grpc' API endpoint property instead of the 'url'.");
            }
            if (jaeger.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                log.warn("Jaeger Exporter is enabled but 'grpc' is not set.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            JaegerGrpcExporterSettings settings = configuration.getExporters().getTracing().getJaegerGrpc();
            log.info("Starting Jaeger gRPC Exporter with grpc '{}'", settings.getGrpc());

            spanExporter = JaegerGrpcSpanExporter.builder().setEndpoint(settings.getGrpc()).build();

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
