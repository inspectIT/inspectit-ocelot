package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.LoggingMetricsExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

/**
 * Service for the {@link io.opentelemetry.exporter.logging.LoggingMetricExporter}
 */
@Component
@Slf4j
public class LoggingMetricExporterService extends DynamicallyActivatableService {

    /**
     * The {@link LoggingMetricExporter} for exporting metrics to the log
     */
    private LoggingMetricExporter metricExporter;

    /**
     * The {@link PeriodicMetricReader} for reading metrics to the log
     */
    private MetricReader metricReader;

    public LoggingMetricExporterService() {
        super("exporters.metrics.logging", "metrics.enabled");
    }

    @Override
    protected void init() {
        super.init();

        // create new metric exporter
        metricExporter = LoggingMetricExporter.create();
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid LoggingMetricsExporterSettings logging = configuration.getExporters().getMetrics().getLogging();
        return configuration.getMetrics().isEnabled() && !logging.getEnabled().isDisabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        LoggingMetricsExporterSettings logging = configuration.getExporters().getMetrics().getLogging();
        try {
            metricReader = PeriodicMetricReader.builder(metricExporter)
                    .setInterval(logging.getExportInterval())
                    .build();
            boolean success = openTelemetryController.registerMetricReader(metricReader, getName());
            if (success) {
                log.info("Starting {}", getName());
            } else {
                log.error("Failed to register {} at the OpenTelemetry controller!", getName());
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to start " + getName(), e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        try {
            log.info("Stopping LoggingMetricExporter");
            openTelemetryController.unregisterMetricExporterService(getName());
            return true;
        } catch (Exception e) {
            log.error("Failed to stop LoggingMetricExporter", e);
            return false;
        }
    }
}
