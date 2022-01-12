package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServerBuilder;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

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
        return conf.getExporters().getMetrics().getPrometheus().isEnabled() && conf.getMetrics().isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        val config = configuration.getExporters().getMetrics().getPrometheus();

        try {
            String host = config.getHost();
            int port = config.getPort();
            log.info("Starting Prometheus Exporter on {}:{}", host, port);
            prometheusHttpServerBuilder = PrometheusHttpServer.builder().setHost(host).setPort(port);
            openTelemetryController.registerMetricExporterService(this);
        } catch (Exception e) {
            log.error("Error Starting Prometheus HTTP Endpoint!", e);
            return false;
        }
        return true;
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
