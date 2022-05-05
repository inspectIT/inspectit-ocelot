package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServerBuilder;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.PrometheusExporterSettings;

/**
 * Service for the Prometheus OpenTelemetry exporter.
 * Can be dynamically started and stopped using the exporters.metrics.prometheus.enabled configuration.
 */
@Component
@Slf4j
public class PrometheusExporterService extends DynamicallyActivatableMetricsExporterService {

    private PrometheusHttpServerBuilder prometheusHttpServerBuilder;

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
        PrometheusExporterSettings config = configuration.getExporters().getMetrics().getPrometheus();

        try {
            String host = config.getHost();
            int port = config.getPort();
            prometheusHttpServerBuilder = PrometheusHttpServer.builder().setHost(host).setPort(port);
            boolean success = openTelemetryController.registerMetricExporterService(this);
            if (success) {
                log.info("Starting Prometheus Exporter on {}:{}", host, port);
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
        openTelemetryController.unregisterMetricExporterService(this);
        return true;
    }

    @Override
    public MetricReaderFactory getNewMetricReaderFactory() {
        return prometheusHttpServerBuilder.newMetricReaderFactory();
    }
}
