package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.PrometheusExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

/**
 * Service for the Prometheus OpenTelemetry exporter.
 * Can be dynamically started and stopped using the exporters.metrics.prometheus.enabled configuration.
 */
@Component
@Slf4j
public class PrometheusExporterService extends DynamicallyActivatableService implements MetricReaderProvider {

    /** The current exporter settings */
    private PrometheusExporterSettings settings;

    public PrometheusExporterService() {
        super("exporters.metrics.prometheus", "metrics.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        return conf.getMetrics().isEnabled() && !conf.getExporters()
                .getMetrics()
                .getPrometheus()
                .getEnabled()
                .isDisabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        settings = configuration.getExporters().getMetrics().getPrometheus();

        try {
            boolean success = openTelemetryController.registerMetricReaderProvider(this, getName());

            if (success) {
                log.info("Starting Prometheus Exporter on {}:{}", settings.getHost(), settings.getPort());
            } else {
                log.error("Failed to register {} at the OpenTelemetry controller!", getName());
            }
            return success;
        } catch (Exception e) {
            log.error("Error Starting Prometheus HTTP Endpoint!", e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping Prometheus Exporter");
        openTelemetryController.unregisterMetricExporterService(getName());
        return true;
    }

    @Override
    public MetricReader getNewMetricReader() {
        return PrometheusHttpServer.builder()
                .setHost(settings.getHost())
                .setPort(settings.getPort())
                .build();
    }
}
