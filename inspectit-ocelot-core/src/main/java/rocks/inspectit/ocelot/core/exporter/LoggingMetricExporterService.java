package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.opencensusshim.metrics.OpenCensusMetrics;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
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
     * The {@link SdkMeterProvider}
     */
    private SdkMeterProvider meterProvider;

    /**
     * The {@link DynamicallyActivatableMetricExporter} for exporting metrics to the log
     */
    private DynamicallyActivatableMetricExporter<LoggingMetricExporter> metricExporter;

    /**
     * The {@link PeriodicMetricReader} for reading metrics to the log
     */
    private PeriodicMetricReaderBuilder metricReader;

    public LoggingMetricExporterService() {
        super("exporters.metrics.logging", "metrics.enabled");
    }

    @Override
    protected void init() {
        super.init();

        // create new metric exporter
        metricExporter = DynamicallyActivatableMetricExporter.createLoggingExporter();

        // close the tracer provider when the JVM is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (null != meterProvider) {
                meterProvider.shutdown();
            }
        }));
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

            meterProvider = SdkMeterProvider.builder()
                    .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, configuration.getServiceName())))
                    .registerMetricReader(OpenCensusMetrics.attachTo(metricReader.newMetricReaderFactory()))
                    .buildAndRegisterGlobal();

            // enable the metric exporter
            metricExporter.doEnable();
            log.info("Starting LoggingMetricsExporter");
            return true;
        } catch (Exception e) {
            log.error("Failed to start LoggingMetricExporter", e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        try {
            // close the meter provider
            if (null != meterProvider) {
                // flush all metrics before disabling them
                meterProvider.forceFlush();
                meterProvider.close();
                meterProvider = null;
            }
            metricExporter.doDisable();
            log.info("Stopping LoggingMetricExporter");
            return true;
        } catch (Exception e) {
            log.error("Failed to stop LoggingMetricExporter", e);
            return false;
        }
    }
}
