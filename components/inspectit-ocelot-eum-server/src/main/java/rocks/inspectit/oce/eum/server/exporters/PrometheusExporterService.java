package rocks.inspectit.oce.eum.server.exporters;

import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.opencensus.exporter.stats.prometheus.PrometheusStatsConfiguration;
import io.prometheus.client.exporter.HTTPServer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.model.config.Configuration;

import javax.annotation.PostConstruct;

import static io.prometheus.client.CollectorRegistry.defaultRegistry;

/**
 * Service for the Prometheus OpenCensus exporters.
 * Is enabled, if exporters.metrics.prometheus.enabled is set to "true".
 */
@Component
@Slf4j
public class PrometheusExporterService {

    private HTTPServer prometheusClient = null;

    @Autowired
    Configuration configuration;

    /**
     * Triggered, if exporters.metrics.prometheus.enabled is set to "true".
     */
    @PostConstruct
    private void doEnable() {
        val config = configuration.getExporters().getMetrics().getPrometheus();
        if(config.isEnabled() && config.getHost()!= null && config.getPort() != 0) {
            try {
                String host = config.getHost();
                int port = config.getPort();
                log.info("Starting Prometheus Exporter on {}:{}", host, port);
                PrometheusStatsCollector.createAndRegister(PrometheusStatsConfiguration.builder().setRegistry(defaultRegistry).build());
                prometheusClient = new HTTPServer(host, port, true);
            } catch (Exception e) {
                log.error("Error Starting Prometheus HTTP Endpoint!", e);
                defaultRegistry.clear();
            }
        }
    }
}
