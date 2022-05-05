package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.trace.ZipkinExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

/**
 * Service for the {@link ZipkinSpanExporter ZipKin OpenTelemetry exporter}.
 * Can be dynamically started and stopped using the exporters.trace.zipkin.enabled configuration.
 */
@Component
@Slf4j
public class ZipkinExporterService extends DynamicallyActivatableService {

    @Getter
    private ZipkinSpanExporter spanExporter;

    public ZipkinExporterService() {
        super("exporters.tracing.zipkin", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid ZipkinExporterSettings zipkin = conf.getExporters().getTracing().getZipkin();
        if (conf.getTracing().isEnabled() && !zipkin.getEnabled().isDisabled()) {
            if (StringUtils.hasText(zipkin.getEndpoint())) {
                return true;
            } else if (StringUtils.hasText(zipkin.getUrl())) {
                log.warn("You are using the deprecated property 'url'. This property will be invalid in future releases of InspectIT Ocelot, please use 'endpoint' instead.");
                return true;
            } else if (zipkin.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                log.warn("Zipkin Exporter is enabled but 'endpoint' is not set.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            ZipkinExporterSettings settings = configuration.getExporters().getTracing().getZipkin();
            String endpoint = StringUtils.hasText(settings.getEndpoint()) ? settings.getEndpoint() : settings.getUrl();

            spanExporter = ZipkinSpanExporter.builder().setEndpoint(endpoint).build();

            boolean success = openTelemetryController.registerTraceExporterService(spanExporter, getName());
            if (success) {
                log.info("Starting Zipkin Exporter with endpoint '{}'", endpoint);
            } else {
                log.error("Failed to register {} at the OpenTelemetry controller!", getName());
            }
            return success;
        } catch (Throwable t) {
            log.error("Error creating Zipkin exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping Zipkin Exporter");
        try {
            openTelemetryController.unregisterTraceExporterService(getName());
            if (null != spanExporter) {
                spanExporter.close();
            }
        } catch (Throwable t) {
            log.error("Error disabling Zipkin exporter", t);
        }
        return true;
    }

}
