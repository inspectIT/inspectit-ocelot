package rocks.inspectit.ocelot.core.exporter;

import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.opencensus.exporter.stats.prometheus.PrometheusStatsConfiguration;
import io.prometheus.client.exporter.HTTPServer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import static io.prometheus.client.CollectorRegistry.defaultRegistry;

/**
 * Service for the Prometheus OpenCensus exporter.
 * Can be dynamically started and stopped using the exporters.metrics.prometheus.enabled configuration.
 */
@Component
@Slf4j
public class PrometheusExporterService extends DynamicallyActivatableService {

    private HTTPServer prometheusClient = null;

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
            PrometheusStatsCollector.createAndRegister(PrometheusStatsConfiguration.builder().setRegistry(defaultRegistry).build());
            prometheusClient = new HTTPServer(host, port, true);
        } catch (Exception e) {
            log.error("Error Starting Prometheus HTTP Endpoint!", e);
            defaultRegistry.clear();
            return false;
        }
        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping Prometheus Exporter");
        if (prometheusClient != null) {
            prometheusClient.stop();
            defaultRegistry.clear();
        }
        return true;
    }
}
