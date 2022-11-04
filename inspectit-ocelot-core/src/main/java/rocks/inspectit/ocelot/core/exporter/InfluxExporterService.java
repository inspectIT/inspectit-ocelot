package rocks.inspectit.ocelot.core.exporter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.metrics.InfluxExporterSettings;
import rocks.inspectit.ocelot.core.metrics.percentiles.PercentileViewManager;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.opencensus.influx.InfluxExporter;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Uses the {@link InfluxExporter} to directly push metrics into a given InfluxDB version 1.x .
 */
@Slf4j
@Component
public class InfluxExporterService extends DynamicallyActivatableService {

    /**
     * Dummy user for downwards compatibility
     */
    public final static String DUMMY_USER = "user";

    /**
     * Dummy password for downwards compatibility
     */
    public final static String DUMMY_PASSWORD = "password";

    private static Logger LOGGER = Logger.getLogger(InfluxExporterService.class.getName());

    @Autowired
    private ScheduledExecutorService executor;

    @Autowired
    private PercentileViewManager percentileViewManager;

    /**
     * The currently active influx exporter, null if none is active.
     */
    private InfluxExporter activeExporter;

    /**
     * A task regularly invoking activeExporter.export() at the configured interval.
     */
    private Future exporterTask;

    public InfluxExporterService() {
        super("exporters.metrics.influx", "metrics.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        InfluxExporterSettings influx = conf.getExporters().getMetrics().getInflux();
        if (conf.getMetrics().isEnabled() && !influx.getEnabled().isDisabled()) {
            if (StringUtils.hasText(influx.getEndpoint())) {
                return true;
            } else if (StringUtils.hasText(influx.getUrl())) {
                log.warn("You are using the deprecated property 'url'. This property will be invalid in future releases of InspectIT Ocelot, please use 'endpoint' instead.");
                return true;
            } else if (influx.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                log.warn("InfluxDB Exporter is enabled but 'endpoint' is not set.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        InfluxExporterSettings influx = configuration.getExporters().getMetrics().getInflux();
        String user = influx.getUser();
        String password = influx.getPassword();

        String endpoint = StringUtils.hasText(influx.getEndpoint()) ? influx.getEndpoint() : influx.getUrl();
        // check user and password, which are not allowed to be null as of v1.15.0
        if (null == user) {
            user = DUMMY_USER;
            password = DUMMY_PASSWORD;
            LOGGER.warning(String.format("You are using the InfluxDB exporter without specifying 'user' and 'password'. Since v1.15.0, 'user' and 'password' are mandatory. Will be using the dummy user '%s' and dummy password '%s'.", DUMMY_USER, DUMMY_PASSWORD));
        }
        log.info("Starting InfluxDB Exporter to '{}:{}' on '{}'", influx.getDatabase(), influx.getRetentionPolicy(), endpoint);
        activeExporter = InfluxExporter.builder()
                .url(endpoint)
                .database(influx.getDatabase())
                .retention(influx.getRetentionPolicy())
                .user(user)
                .password(password)
                .createDatabase(influx.isCreateDatabase())
                .exportDifference(influx.isCountersAsDifferences())
                .measurementNameProvider(percentileViewManager::getMeasureNameForSeries)
                .bufferSize(influx.getBufferSize())
                .build();
        exporterTask = executor.scheduleAtFixedRate(activeExporter::export, 0, influx.getExportInterval()
                .toMillis(), TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    protected boolean doDisable() {
        if (exporterTask != null) {
            log.info("Stopping InfluxDB Exporter");
            exporterTask.cancel(false);
        }
        if (activeExporter != null) {
            // perform a final export before closing to ensure all metrics are written
            activeExporter.export();
            activeExporter.close();
            activeExporter = null;
        }
        return true;
    }
}
