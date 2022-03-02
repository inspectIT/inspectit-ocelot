package rocks.inspectit.oce.eum.server.exporters;

import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.opencensus.exporter.stats.prometheus.PrometheusStatsConfiguration;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Service for the Prometheus OpenCensus exporters.
 * Is enabled, if exporters.metrics.prometheus.enabled is set to ENABLED or IF_CONFIGURED.
 */
@Component
@Slf4j
public class PrometheusExporterService {

    private HTTPServer prometheusClient = null;

    @Autowired
    private EumServerConfiguration configuration;

    @PostConstruct
    private void doEnable() {
        val config = configuration.getExporters().getMetrics().getPrometheus();
        if (!config.getEnabled().isDisabled()) {
            try {
                String host = config.getHost();
                int port = config.getPort();
                log.info("Starting Prometheus Exporter on {}:{}", host, port);
                PrometheusStatsCollector.createAndRegister(PrometheusStatsConfiguration.builder()
                        .setRegistry(CollectorRegistry.defaultRegistry)
                        .build());
                prometheusClient = new HTTPServer(host, port, true);
            } catch (Exception e) {
                log.error("Error Starting Prometheus HTTP Endpoint!", e);
                CollectorRegistry.defaultRegistry.clear();
            }
        }
    }

    @PreDestroy
    protected boolean doDisable() {
        log.info("Stopping Prometheus Exporter");
        if (prometheusClient != null) {
            prometheusClient.stop();
            CollectorRegistry.defaultRegistry.clear();
        }
        return true;
    }
}
