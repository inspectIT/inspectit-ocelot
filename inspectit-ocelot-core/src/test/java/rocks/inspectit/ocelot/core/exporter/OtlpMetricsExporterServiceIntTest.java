package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.CompressionMethod;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.metrics.OtlpMetricsExporterSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test class for {@link OtlpMetricsExporterService}
 */
public class OtlpMetricsExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static final String OTLP_METRICS_PATH = "/v1/metrics";

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create()
            .captureForType(OtlpMetricsExporterService.class, org.slf4j.event.Level.WARN);

    @Autowired
    OtlpMetricsExporterService service;

    @Autowired
    InspectitEnvironment environment;

    String measure = "my-counter";
    String tagKeyGrpc = "otlp-grpc-metrics-test";
    String tagKeyHttp = "otlp-grpc-metrics-test";
    String tagVal = "random-val";
    int metricVal = 1337;

    @BeforeEach
    void clearRequests() {
        grpcServer.metricRequests.clear();
    }

    @DirtiesContext
    @Test
    void verifyMetricsWrittenGrpc() {
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            mps.setProperty("inspectit.exporters.metrics.otlp.export-interval", "500ms");
            mps.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
        });

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        recordMetricsAndFlush(measure, metricVal, tagKeyGrpc, tagVal);

        awaitMetricsExported(measure, metricVal, tagKeyGrpc, tagVal);
    }

    @DirtiesContext
    @Test
    void verifyMetricsWrittenHttp() {
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_HTTP_PORT, OTLP_METRICS_PATH));
            mps.setProperty("inspectit.exporters.metrics.otlp.export-interval", "500ms");
            mps.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.HTTP_PROTOBUF);
        });

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        recordMetricsAndFlush(measure, metricVal, tagKeyHttp, tagVal);

        awaitMetricsExported(measure, metricVal, tagKeyHttp, tagVal);
    }



    @DirtiesContext
    @Test
    void testNoEndpointSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.otlp.endpoint", "");
            props.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
            props.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'endpoint'");
    }

    @DirtiesContext
    @Test
    void testNoProtocolSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.otlp.protocol", "");
            props.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'protocol'");
    }

    @Test
    void defaultSettings() {
        AssertionsForClassTypes.assertThat(service.isEnabled()).isFalse();
        OtlpMetricsExporterSettings otlp = environment.getCurrentConfig().getExporters().getMetrics().getOtlp();
        assertThat(otlp.getEnabled().equals(ExporterEnabledState.IF_CONFIGURED));
        assertThat(otlp.getEndpoint()).isNullOrEmpty();
        assertThat(otlp.getProtocol()).isNull();
        assertThat(otlp.getPreferredTemporality()).isEqualTo(AggregationTemporality.CUMULATIVE);
        assertThat(otlp.getHeaders()).isNullOrEmpty();
        assertThat(otlp.getCompression()).isEqualTo(CompressionMethod.NONE);
        assertThat(otlp.getTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @DirtiesContext
    @Test
    void testAggregationTemporalityCumulative(){
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            mps.setProperty("inspectit.exporters.metrics.otlp.export-interval", "500ms");
            mps.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
            mps.setProperty("inspectit.exporters.metrics.otlp.preferredTemporality", AggregationTemporality.CUMULATIVE);
        });

        assertThat(service.isEnabled()).isTrue();

        recordMetricsAndFlush(measure, 1, "key", "val");
        recordMetricsAndFlush(measure, 2, "key", "val");

        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(grpcServer.metricRequests.stream()).anyMatch(mReq -> mReq.getResourceMetricsList()
                        .stream()
                        .anyMatch(rm ->
                                // check for the "my-counter" metrics
                                rm.getInstrumentationLibraryMetrics(0).getMetrics(0).getName().equals("my-counter")
                                        // check for the specific attribute and value
                                        && rm.getInstrumentationLibraryMetrics(0)
                                        .getMetricsList()
                                        .stream()
                                        .anyMatch(metric -> metric.getSum()
                                                .getDataPointsList()
                                                .stream()
                                                .anyMatch(d -> d.getAsInt() == 3)))));
    }

    @DirtiesContext
    @Test
    void testAggregationTemporalityDelta(){
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            mps.setProperty("inspectit.exporters.metrics.otlp.export-interval", "500ms");
            mps.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
            mps.setProperty("inspectit.exporters.metrics.otlp.preferredTemporality", AggregationTemporality.DELTA);
        });

        assertThat(service.isEnabled()).isTrue();

        recordMetricsAndFlush(measure, 1, "key", "val");
        recordMetricsAndFlush(measure, 2, "key", "val");

        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(grpcServer.metricRequests.stream()).anyMatch(mReq -> mReq.getResourceMetricsList()
                        .stream()
                        .anyMatch(rm ->
                                // check for the "my-counter" metrics
                                rm.getInstrumentationLibraryMetrics(0).getMetrics(0).getName().equals("my-counter")
                                        // check for the specific attribute and value
                                        && rm.getInstrumentationLibraryMetrics(0)
                                        .getMetricsList()
                                        .stream()
                                        .anyMatch(metric -> metric.getSum()
                                                .getDataPointsList()
                                                .stream()
                                                .anyMatch(d -> d.getAsInt() == 2)))));

    }

    @DirtiesContext
    @Test
    void testHeaders(){
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            mps.setProperty("inspectit.exporters.metrics.otlp.export-interval", "500ms");
            mps.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
            mps.setProperty("inspectit.exporters.metrics.otlp.headers", new HashMap<String, String>(){{put("my-header-key","my-header-value");}});
        });
        assertThat(service.isEnabled()).isTrue();
        assertThat(environment.getCurrentConfig().getExporters().getMetrics().getOtlp().getHeaders()).containsEntry("my-header-key","my-header-value");
    }

    @DirtiesContext
    @Test
    void testCompression() {
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
            properties.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            properties.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            properties.setProperty("inspectit.exporters.metrics.otlp.compression", CompressionMethod.GZIP);
        });
        assertThat(service.isEnabled()).isTrue();
        assertThat(environment.getCurrentConfig().getExporters().getMetrics().getOtlp().getCompression()).isEqualTo(CompressionMethod.GZIP);
    }
}
