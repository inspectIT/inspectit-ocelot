package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.trace.ZipkinExporterSettings;

import javax.validation.Valid;

/**
 * Service for the {@link ZipkinSpanExporter ZipKin OpenTelemetry exporter}.
 * Can be dynamically started and stopped using the exporters.trace.zipkin.enabled configuration.
 */
@Component
@Slf4j
public class ZipkinExporterService extends DynamicallyActivatableTraceExporterService {

    @Getter
    private ZipkinSpanExporter spanExporter;

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

            // create span exporter
            spanExporter = ZipkinSpanExporter.builder().setEndpoint(settings.getUrl()).build();

            // register
            openTelemetryController.registerTraceExporterService(this);
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
            openTelemetryController.unregisterTraceExporterService(this);
            if (null != spanExporter) {
                spanExporter.close();
            }
        } catch (Throwable t) {
            log.error("Error disabling Zipkin exporter", t);
        }
        return true;
    }

}
