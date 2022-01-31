package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.trace.JaegerExporterSettings;

import javax.validation.Valid;

/**
 * Service for the {@link JaegerThriftSpanExporter}.
 * Can be dynamically started and stopped using the exporters.trace.jaeger.enabled configuration.
 */
@Component
@Slf4j
public class JaegerExporterService extends DynamicallyActivatableTraceExporterService {

    private JaegerGrpcSpanExporter grpcSpanExporter;

    @Getter
    private JaegerThriftSpanExporter spanExporter;

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
        if (conf.getTracing().isEnabled() && jaeger.isEnabled()) {
            if (!StringUtils.isEmpty(jaeger.getUrl())) {
                return true;
            } else if (StringUtils.isNotEmpty(jaeger.getGrpc())) {
                // print warning if user used wrong setup
                log.warn("In order to use Jaeger span exporter, please specify the HTTP URL endpoint property instead of the gRPC.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            JaegerExporterSettings settings = configuration.getExporters().getTracing().getJaeger();
            log.info("Starting Jaeger Thrift Exporter with url '{}' (grpc '{}')", settings.getUrl(), settings.getGrpc());

            // create span exporter
            spanExporter = JaegerThriftSpanExporter.builder().setEndpoint(settings.getUrl()).build();

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
