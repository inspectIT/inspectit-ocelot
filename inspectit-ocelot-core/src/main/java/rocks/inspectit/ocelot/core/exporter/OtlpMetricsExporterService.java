package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.metrics.OtlpMetricsExporterSettings;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * Service for {@link io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter}/{@link io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter}.
 * Can be dynamically started and stopped using the exporters.metrics.otlp.enabled configuration
 */
@Component
@Slf4j
public class OtlpMetricsExporterService extends DynamicallyActivatableMetricsExporterService {

    private final List<TransportProtocol> SUPPORTED_PROTOCOLS = Arrays.asList(TransportProtocol.GRPC, TransportProtocol.HTTP_PROTOBUF);

    /**
     * The {@link MetricExporter} for exporting metrics via OTLP
     */
    private MetricExporter metricExporter;

    /**
     * The {@link PeriodicMetricReaderBuilder} for reading metrics to the log
     */
    private PeriodicMetricReaderBuilder metricReaderBuilder;

    public OtlpMetricsExporterService() {
        super("metrics.enabled", "exporters.metrics.otlp");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid OtlpMetricsExporterSettings otlp = configuration.getExporters().getMetrics().getOtlp();
        if (configuration.getMetrics().isEnabled() && !otlp.getEnabled().isDisabled()) {
            if (SUPPORTED_PROTOCOLS.contains(otlp.getProtocol())) {

                if (StringUtils.hasText(otlp.getEndpoint())) {
                    return true;
                } else if (StringUtils.hasText(otlp.getEndpoint())) {
                    log.warn("You are using the deprecated property 'url'. This property will be invalid in future releases of InspectIT Ocelot, please use 'endpoint' instead.");
                    return true;
                }
            }
            if (otlp.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                if (!SUPPORTED_PROTOCOLS.contains(otlp.getProtocol())) {
                    log.warn("OTLP Metric Exporter is enabled, but wrong 'protocol' is specified. Supported values are ", Arrays.toString(SUPPORTED_PROTOCOLS.stream()
                            .map(transportProtocol -> transportProtocol.getConfigRepresentation())
                            .toArray()));
                }
                if (!StringUtils.hasText(otlp.getEndpoint()) && !StringUtils.hasText(otlp.getEndpoint())) {
                    log.warn("OTLP Metric Exporter is enabled but 'endpoint' is not set.");
                }
            }
        }

        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            OtlpMetricsExporterSettings otlp = configuration.getExporters().getMetrics().getOtlp();

            AggregationTemporality preferredTemporality = getPreferredTemporality(otlp);

            switch (otlp.getProtocol()) {
                case GRPC: {
                    metricExporter = OtlpGrpcMetricExporter.builder()
                            .setPreferredTemporality(preferredTemporality)
                            .setEndpoint(otlp.getEndpoint()).build();
                    break;
                }
                case HTTP_PROTOBUF: {
                    metricExporter = OtlpHttpMetricExporter.builder()
                            .setPreferredTemporality(preferredTemporality)
                            .setEndpoint(otlp.getEndpoint()).build();
                    break;
                }
            }
            metricReaderBuilder = PeriodicMetricReader.builder(metricExporter).setInterval(otlp.getExportInterval());

            boolean success = openTelemetryController.registerMetricExporterService(this);
            if (success) {
                log.info("Starting {}", getName());
            } else {
                log.error("Failed to register {} at the OpenTelemetry controller!", getName());
            }
            return success;
        } catch (Exception e) {
            log.error("Error creatig OTLP metrics exporter service", e);
            return false;
        }
    }

    private static AggregationTemporality getPreferredTemporality(OtlpMetricsExporterSettings otlp) {
        AggregationTemporality preferredTemporality;
        try {
           preferredTemporality = AggregationTemporality.valueOf(otlp.getPreferredTemporality());
        }
        catch ( IllegalArgumentException e) {
            preferredTemporality = AggregationTemporality.CUMULATIVE;
            log.error("Unable to set preferred Temporality of value {}. Falling back to {}. Valid values are {}", otlp.getPreferredTemporality(), preferredTemporality, AggregationTemporality.values());
        }
        return preferredTemporality;
    }

    @Override
    protected boolean doDisable() {
        try {
            log.info("Stopping OtlpMetricsExporter");
            openTelemetryController.unregisterMetricExporterService(this);
            return true;
        } catch (Exception e) {
            log.error("Failed to stop OtlpMetricsExporter", e);
            return false;
        }
    }

    @Override
    public MetricReaderFactory getNewMetricReaderFactory() {
        return metricReaderBuilder.newMetricReaderFactory();
    }
}
