package rocks.inspectit.ocelot.core.exporter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.InfluxExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.opencensus.influx.InfluxExporter;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Uses the {@link InfluxExporter} to directly push metrics into a given InfluxDB version 1.x
 */
@Slf4j
@Component
public class InfluxExporterService extends DynamicallyActivatableService {

    @Autowired
    private ScheduledExecutorService executor;

    private InfluxExporter activeExporter;

    private Future exporterTask;


    public InfluxExporterService() {
        super("exporters.metrics.influx", "metrics.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        InfluxExporterSettings influx = conf.getExporters().getMetrics().getInflux();
        return conf.getMetrics().isEnabled()
                && influx.isEnabled()
                && !StringUtils.isEmpty(influx.getUrl())
                && !StringUtils.isEmpty(influx.getDatabase())
                && !StringUtils.isEmpty(influx.getRetentionPolicy());
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        InfluxExporterSettings influx = configuration.getExporters().getMetrics().getInflux();
        log.info("Starting Influx  to '{}:{}' on '{}'", influx.getDatabase(), influx.getRetentionPolicy(), influx.getUrl());
        activeExporter = InfluxExporter.builder()
                .url(influx.getUrl())
                .database(influx.getDatabase())
                .retention(influx.getRetentionPolicy())
                .user(influx.getUser())
                .password(influx.getPassword())
                .createDatabase(influx.isCreateDatabase())
                .build();
        exporterTask = executor.scheduleAtFixedRate(activeExporter::export, 0, influx.getExportInterval().toMillis(), TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    protected boolean doDisable() {
        if (exporterTask != null) {
            log.info("Stopping Influx Exporter");
            exporterTask.cancel(false);
        }
        if (activeExporter != null) {
            activeExporter.close();
            activeExporter = null;
        }
        return true;
    }
}
