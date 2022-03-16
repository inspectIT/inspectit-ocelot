package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test class for {@link OtlpGrpcMetricsExporterService}
 */
public class OtlpGrpcMetricsExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static final String OTLP_GRPC_METRICS_PATH = "/v1/metrics";

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create()
            .captureForType(OtlpGrpcMetricsExporterService.class, org.slf4j.event.Level.WARN);

    @Autowired
    OtlpGrpcMetricsExporterService service;

    @Autowired
    InspectitEnvironment environment;

    String tagKey = "otlp-grpc-metrics-test";

    String tagVal = "random-val";

    int metricVal = 1337;

    @Test
    void verifyMetricsWritten() {
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.metrics.otlp-grpc.url", getEndpoint(COLLECTOR_OTLP_GRPC_PORT, OTLP_GRPC_METRICS_PATH));
            mps.setProperty("inspectit.exporters.metrics.otlp-grpc.export-interval", "500ms");
            mps.setProperty("inspectit.exporters.metrics.otlp-grpc.enabled", ExporterEnabledState.ENABLED);
        });

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        recordMetricsAndFlush(metricVal, tagKey, tagVal);

        awaitMetricsExported(metricVal, tagKey, tagVal);
    }

    @DirtiesContext
    @Test
    void testNoUrlSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.otlp-grpc.url", "");
            props.setProperty("inspectit.exporters.metrics.otlp-grpc.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'url'");
    }

}
