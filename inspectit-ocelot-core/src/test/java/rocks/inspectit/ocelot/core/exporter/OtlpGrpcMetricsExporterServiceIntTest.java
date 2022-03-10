package rocks.inspectit.ocelot.core.exporter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class OtlpGrpcMetricsExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static final String OTLP_GRPC_METRICS_PATH = "/v1/metrics";

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
            mps.setProperty("inspectit.exporters.metrics.otlp-grpc.enabled", true);
        });

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        recordMetricsAndFlush(metricVal, tagKey, tagVal);

        awaitMetricsExported(metricVal, tagKey, tagVal);
    }

}
