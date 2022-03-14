package rocks.inspectit.ocelot.core.exporter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.trace.JaegerExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

/**
 * Service for the Jaeger OpenCensus exporter.
 * Can be dynamically started and stopped using the exporters.trace.jaeger.enabled configuration.
 */
@Component
@Slf4j
public class JaegerExporterService extends DynamicallyActivatableService {

    public JaegerExporterService() {
        super("exporters.tracing.jaeger", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid JaegerExporterSettings jaeger = conf.getExporters().getTracing().getJaeger();
        if (conf.getTracing().isEnabled() && !jaeger.getEnabled().isDisabled()) {
            if (StringUtils.hasText(jaeger.getUrl())) {
                return true;
            } else if (StringUtils.hasText(jaeger.getGrpc())) {
                // print warning if user used wrong setup
                log.warn("In order to use Jaeger span exporter, please specify the HTTP URL endpoint property instead of the gRPC.");
            }
            if (jaeger.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                log.warn("Jaeger Exporter is enabled but 'url' is not set.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            JaegerExporterSettings settings = configuration.getExporters().getTracing().getJaeger();
            log.info("Starting Jaeger Exporter with url '{}'", settings.getUrl());
            // TODO re-implement with OTel
            /*
            JaegerTraceExporter.createAndRegister(
                    JaegerExporterConfiguration.builder().setThriftEndpoint(settings.getUrl()).setServiceName(settings.getServiceName()).build());
            */
            return true;
        } catch (Throwable t) {
            log.error("Error creating Jaeger exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping Jaeger Exporter");
        try {
            // TODO: reimplement with OTel
            /*
            JaegerTraceExporter.unregister();
            */
        } catch (Throwable t) {
            log.error("Error disabling Jaeger exporter", t);
        }
        return true;
    }
}
