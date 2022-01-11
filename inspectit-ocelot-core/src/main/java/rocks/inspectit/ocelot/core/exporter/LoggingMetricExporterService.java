package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.LoggingMetricsExporterSettings;

import javax.validation.Valid;

/**
 * Service for the {@link io.opentelemetry.exporter.logging.LoggingMetricExporter}
 */
@Component
@Slf4j
public class LoggingMetricExporterService extends DynamicallyActivatableMetricsExporterService {


    /**
     * The {@link DynamicallyActivatableMetricExporter} for exporting metrics to the log
     */
    private DynamicallyActivatableMetricExporter<LoggingMetricExporter> metricExporter;

    /**
     * The {@link PeriodicMetricReader} for reading metrics to the log
     */
    private PeriodicMetricReaderBuilder metricReader;

    @Getter
    MetricReaderFactory metricReaderFactory;

    public LoggingMetricExporterService() {
        super("exporters.metrics.logging", "metrics.enabled");
    }

    @Override
    protected void init() {
        super.init();

        // create new metric exporter
        metricExporter = DynamicallyActivatableMetricExporter.createLoggingExporter();
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid LoggingMetricsExporterSettings logging = configuration.getExporters().getMetrics().getLogging();
        return configuration.getMetrics().isEnabled() && logging.isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        LoggingMetricsExporterSettings logging = configuration.getExporters().getMetrics().getLogging();
        try {
            // build and register the MeterProvider
            metricReader = PeriodicMetricReader.builder(metricExporter).setInterval(logging.getExportInterval());
            metricReaderFactory = metricReader.newMetricReaderFactory();
            boolean success = openTelemetryController.registerMetricExporterService(this);
            if (success) {
                // enable the metric exporter
                metricExporter.doEnable();
                log.info("Starting {}", getClass().getSimpleName());
            } else {
                log.error("Failed to register {} at {}!", getClass().getSimpleName(), openTelemetryController.getClass()
                        .getSimpleName());
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to start " + getClass().getSimpleName(), e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        try {
            metricExporter.doDisable();
            log.info("Stopping LoggingMetricExporter");
            openTelemetryController.unregisterMetricExporterService(this);
            return true;
        } catch (Exception e) {
            log.error("Failed to stop LoggingMetricExporter", e);
            return false;
        }
    }
}
