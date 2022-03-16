package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.metrics.OtlpGrpcMetricsExporterSettings;

import javax.validation.Valid;

/**
 * Service for {@link io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter}.
 * Can be dynamically started and stopped using the exporters.metrics.otlp-grpc.enabled configuration
 */
@Component
@Slf4j
public class OtlpGrpcMetricsExporterService extends DynamicallyActivatableMetricsExporterService {

    /**
     * The {@link OtlpGrpcMetricExporter} for exporting metrics via OTLP gRPC
     */
    private OtlpGrpcMetricExporter metricExporter;

    /**
     * The {@link PeriodicMetricReaderBuilder} for reading metrics to the log
     */
    private PeriodicMetricReaderBuilder metricReaderBuilder;

    public OtlpGrpcMetricsExporterService() {
        super("metrics.enabled", "exporters.metrics.otlpGrpc");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid OtlpGrpcMetricsExporterSettings otlp = configuration.getExporters().getMetrics().getOtlpGrpc();
        if (configuration.getMetrics().isEnabled() && !otlp.getEnabled().isDisabled()) {
            if (StringUtils.hasText(otlp.getUrl())) {
                return true;
            } else if (otlp.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                log.warn("OTLP gRPC Metric Exporter is enabled but 'url' is not set.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            // build and register exporter service
            OtlpGrpcMetricsExporterSettings otlp = configuration.getExporters().getMetrics().getOtlpGrpc();
            metricExporter = OtlpGrpcMetricExporter.builder().setEndpoint(otlp.getUrl()).build();
            metricReaderBuilder = PeriodicMetricReader.builder(metricExporter).setInterval(otlp.getExportInterval());

            boolean success = openTelemetryController.registerMetricExporterService(this);
            if (success) {
                log.info("Starting {}", getClass().getSimpleName());
            } else {
                log.error("Failed to register {} at {}!", getClass().getSimpleName(), openTelemetryController.getClass()
                        .getSimpleName());
            }
            return success;
        } catch (Exception e) {
            log.error("Error creatig OTLP metrics exporter service", e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        try {
            log.info("Stopping OtlpGrpcMetricsExporter");
            openTelemetryController.unregisterMetricExporterService(this);
            return true;
        } catch (Exception e) {
            log.error("Failed to stop OtlpGrpcMetricsExporter", e);
            return false;
        }
    }

    @Override
    public MetricReaderFactory getNewMetricReaderFactory() {
        return metricReaderBuilder.newMetricReaderFactory();
    }
}
