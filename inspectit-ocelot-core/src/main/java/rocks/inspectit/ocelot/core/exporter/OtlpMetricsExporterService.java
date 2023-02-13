package rocks.inspectit.ocelot.core.exporter;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.*;
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
import java.util.Map;

/**
 * Service for {@link io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter}/{@link io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter}.
 * Can be dynamically started and stopped using the exporters.metrics.otlp.enabled configuration
 */
@Component
@Slf4j
public class OtlpMetricsExporterService extends DynamicallyActivatableMetricsExporterService {

    private final List<TransportProtocol> SUPPORTED_PROTOCOLS = Arrays.asList(TransportProtocol.GRPC, TransportProtocol.HTTP_PROTOBUF);

    @VisibleForTesting
    /**
     * The {@link MetricExporter} for exporting metrics via OTLP
     */
            MetricExporter metricExporter;

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
            AggregationTemporalitySelector aggregationTemporalitySelector = otlp.getPreferredTemporality() == AggregationTemporality.DELTA ? AggregationTemporalitySelector.deltaPreferred() : AggregationTemporalitySelector.alwaysCumulative();

            switch (otlp.getProtocol()) {
                case GRPC: {
                    OtlpGrpcMetricExporterBuilder metricExporterBuilder = OtlpGrpcMetricExporter.builder()
                            .setAggregationTemporalitySelector(aggregationTemporalitySelector)
                            .setEndpoint(otlp.getEndpoint())
                            .setCompression(otlp.getCompression().toString())
                            .setTimeout(otlp.getTimeout());
                    if (otlp.getHeaders() != null) {
                        for (Map.Entry<String, String> headerEntry : otlp.getHeaders().entrySet()) {
                            metricExporterBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
                        }
                    }
                    metricExporter = metricExporterBuilder.build();
                    break;
                }
                case HTTP_PROTOBUF: {
                    OtlpHttpMetricExporterBuilder metricExporterBuilder = OtlpHttpMetricExporter.builder()
                            .setAggregationTemporalitySelector(aggregationTemporalitySelector)
                            .setEndpoint(otlp.getEndpoint())
                            .setCompression(otlp.getCompression().toString())
                            .setTimeout(otlp.getTimeout());
                    if (otlp.getHeaders() != null) {
                        for (Map.Entry<String, String> headerEntry : otlp.getHeaders().entrySet()) {
                            metricExporterBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
                        }
                    }
                    metricExporter = metricExporterBuilder.build();
                    break;
                }
            }
            metricReaderBuilder = PeriodicMetricReader.builder(metricExporter).setInterval(otlp.getExportInterval());

            boolean success = openTelemetryController.registerMetricExporterService(this);
            if (success) {
                log.info("Starting {} with protocol {} on endpoint {}", getName(), otlp.getProtocol(), otlp.getEndpoint());
            } else {
                log.error("Failed to register {} at the OpenTelemetry controller!", getName());
            }
            return success;
        } catch (Exception e) {
            log.error("Error creating OTLP metrics exporter service", e);
            return false;
        }
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
    public MetricReader getNewMetricReader() {
        return metricReaderBuilder.build();
    }
}
