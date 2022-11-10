package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.metrics.OtlpMetricsExporterSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

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

        recordMetricsAndFlush(metricVal, tagKeyGrpc, tagVal);

        awaitMetricsExported(metricVal, tagKeyGrpc, tagVal);
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

        recordMetricsAndFlush(metricVal, tagKeyHttp, tagVal);

        awaitMetricsExported(metricVal, tagKeyHttp, tagVal);
    }

    @DirtiesContext
    @Test
    void invalidPreferredTemporalitySet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            props.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            props.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
            props.setProperty("inspectit.exporters.metrics.otlp.preferredTemporality", "invalid");
        });
        warnLogs.assertContains("invalid");
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
    }
}
