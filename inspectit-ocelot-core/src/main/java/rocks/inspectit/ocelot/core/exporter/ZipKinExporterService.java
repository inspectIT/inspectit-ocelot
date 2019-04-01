package rocks.inspectit.ocelot.core.exporter;

import io.opencensus.exporter.trace.zipkin.ZipkinTraceExporter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.model.exporters.trace.ZipKinExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

/**
 * Service for the ZipKin OpenCensus exporter.
 * Can be dynamically started and stopped using the exporters.trace.zipkin.enabled configuration.
 */
@Component
@Slf4j
public class ZipKinExporterService extends DynamicallyActivatableService {

    public ZipKinExporterService() {
        super("exporters.tracing.zipkin", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid ZipKinExporterSettings zipkin = conf.getExporters().getTracing().getZipkin();
        return zipkin.isEnabled() && !StringUtils.isEmpty(zipkin.getUrl()) && conf.getTracing().isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            ZipKinExporterSettings settings = configuration.getExporters().getTracing().getZipkin();
            log.info("Starting ZipKin Exporter with url '{}'", settings.getUrl());
            ZipkinTraceExporter.createAndRegister(settings.getUrl(), settings.getServiceName());
            return true;
        } catch (Throwable t) {
            log.error("Error creating ZipKin exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping ZipKin Exporter");
        try {
            ZipkinTraceExporter.unregister();
        } catch (Throwable t) {
            log.error("Error disabling ZipKin exporter", t);
        }
        return true;
    }
}
