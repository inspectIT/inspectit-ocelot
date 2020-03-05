package rocks.inspectit.oce.eum.server.exporters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
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
    private EumServerConfiguration config;

    /**
     * The currently active influx exporter, null if none is active.
     */
    private InfluxExporter activeExporter;

    /**
     * A task regularly invoking activeExporter.export() at the configured interval.
     */
    private Future exporterTask;

    private boolean shouldEnable() {
        InfluxExporterSettings influx = config.getExporters().getMetrics().getInflux();
        return influx.isEnabled()
                && !StringUtils.isEmpty(influx.getUrl())
                && !StringUtils.isEmpty(influx.getDatabase())
                && !StringUtils.isEmpty(influx.getRetentionPolicy());
    }

    @PostConstruct
    private void doEnable() {
        InfluxExporterSettings influx = config.getExporters().getMetrics().getInflux();
        if (shouldEnable()) {
            log.info("Starting InfluxDB Exporter to '{}:{}' on '{}'", influx.getDatabase(), influx.getRetentionPolicy(), influx.getUrl());
            activeExporter = InfluxExporter.builder()
                    .url(influx.getUrl())
                    .database(influx.getDatabase())
                    .retention(influx.getRetentionPolicy())
                    .user(influx.getUser())
                    .password(influx.getPassword())
                    .createDatabase(influx.isCreateDatabase())
                    .build();
            exporterTask = executor.scheduleAtFixedRate(activeExporter::export, 0, influx.getExportInterval().toMillis(), TimeUnit.MILLISECONDS);
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
