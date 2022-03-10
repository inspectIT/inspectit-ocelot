package rocks.inspectit.ocelot.core.exporter;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.trace.ZipkinExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

/**
 * Service for the ZipKin OpenCensus exporter.
 * Can be dynamically started and stopped using the exporters.trace.zipkin.enabled configuration.
 */
@Component
@Slf4j
public class ZipkinExporterService extends DynamicallyActivatableService {

    public ZipkinExporterService() {
        super("exporters.tracing.zipkin", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid ZipkinExporterSettings zipkin = conf.getExporters().getTracing().getZipkin();
        if (conf.getTracing().isEnabled() && !zipkin.getEnabled().isDisabled()) {
            if (StringUtils.hasText(zipkin.getUrl())) {
                return true;
            } else if (zipkin.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                log.warn("Zipkin Exporter is enabled but 'url' is not set.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            ZipkinExporterSettings settings = configuration.getExporters().getTracing().getZipkin();
            log.info("Starting Zipkin Exporter with url '{}'", settings.getUrl());
            // TODO: implement OTel equivalent
            /*
            ZipkinTraceExporter.createAndRegister(
                    ZipkinExporterConfiguration.builder().setV2Url(settings.getUrl()).setServiceName(settings.getServiceName()).build());
             */
            return true;
        } catch (Throwable t) {
            log.error("Error creating Zipkin exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping Zipkin Exporter");
        try {
            // TODO: implement OTel equivalent
            /*
            ZipkinTraceExporter.unregister();
             */
        } catch (Throwable t) {
            log.error("Error disabling Zipkin exporter", t);
        }
        return true;
    }
}
