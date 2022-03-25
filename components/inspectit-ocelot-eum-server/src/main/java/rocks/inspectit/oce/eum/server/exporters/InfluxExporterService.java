package rocks.inspectit.oce.eum.server.exporters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.metrics.percentiles.TimeWindowViewManager;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.metrics.InfluxExporterSettings;
import rocks.inspectit.opencensus.influx.InfluxExporter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * COPIED FROM THE OCELOT CORE PROJECT!
 * Uses the {@link InfluxExporter} to directly push metrics into a given InfluxDB version 1.x .
 */
@Slf4j
@Component
public class InfluxExporterService {

    @Autowired
    private ScheduledExecutorService executor;

    @Autowired
    private TimeWindowViewManager timeWindowViewManager;

    @Autowired
    private EumServerConfiguration configuration;

    /**
     * The currently active influx exporter, null if none is active.
     */
    private InfluxExporter activeExporter;

    /**
     * A task regularly invoking activeExporter.export() at the configured interval.
     */
    private Future exporterTask;

    private boolean shouldEnable() {
        InfluxExporterSettings influx = configuration.getExporters().getMetrics().getInflux();
        if (!influx.getEnabled().isDisabled()) {
            if (StringUtils.hasText(influx.getEndpoint())) {
                return true;
            } else if (StringUtils.hasText(influx.getUrl())) {
                log.warn("You are using the deprecated property 'url'. This property will be invalid in future releases of InspectIT Ocelot, please use `endpoint` instead.");
                return true;
            } else if (influx.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                log.warn("InfluxDB Exporter is enabled but 'endpoint' is not set.");
            }
        }
        return false;
    }

    @PostConstruct
    private void doEnable() {
        InfluxExporterSettings influx = configuration.getExporters().getMetrics().getInflux();
        if (shouldEnable()) {
            String endpoint = StringUtils.hasText(influx.getEndpoint()) ? influx.getEndpoint() : influx.getUrl();
            log.info("Starting InfluxDB Exporter to '{}:{}' on '{}'", influx.getDatabase(), influx.getRetentionPolicy(), endpoint);
            activeExporter = InfluxExporter.builder()
                    .url(endpoint)
                    .database(influx.getDatabase())
                    .retention(influx.getRetentionPolicy())
                    .user(influx.getUser())
                    .password(influx.getPassword())
                    .createDatabase(influx.isCreateDatabase())
                    .exportDifference(influx.isCountersAsDifferences())
                    .measurementNameProvider(timeWindowViewManager::getMeasureNameForSeries)
                    .bufferSize(influx.getBufferSize())
                    .build();
            exporterTask = executor.scheduleAtFixedRate(activeExporter::export, 0, influx.getExportInterval()
                    .toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    private void doDisable() {
        if (exporterTask != null) {
            log.info("Stopping InfluxDB Exporter");
            exporterTask.cancel(false);
        }
        if (activeExporter != null) {
            activeExporter.close();
            activeExporter = null;
        }
    }
}
