package rocks.inspectit.ocelot.core.exporter;

import io.opencensus.exporter.trace.jaeger.JaegerTraceExporter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.model.exporters.trace.JaegerExporterSettings;
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
        return jaeger.isEnabled()
                && !StringUtils.isEmpty(jaeger.getUrl())
                && conf.getTracing().isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            JaegerExporterSettings settings = configuration.getExporters().getTracing().getJaeger();
            log.info("Starting Jaeger Exporter with url '{}'", settings.getUrl());
            JaegerTraceExporter.createAndRegister(settings.getUrl(), settings.getServiceName());
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
            JaegerTraceExporter.unregister();
        } catch (Throwable t) {
            log.error("Error disabling Jaeger exporter", t);
        }
        return true;
    }
}
