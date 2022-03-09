package rocks.inspectit.ocelot.core.exporter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DirtiesContext
public class OtlpGrpcTraceExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static final String OTLP_GRPC_TRACING_PATH = "/v1/trace";

    @Autowired
    private OtlpGrpcTraceExporterService service;

    @Test
    void verifyTraceSent() {
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.tracing.otlp-grpc.url", getEndpoint(COLLECTOR_OTLP_GRPC_PORT, OTLP_GRPC_TRACING_PATH));
            properties.setProperty("inspectit.exporters.tracing.otlp-grpc.enabled", true);
        });

        System.out.println("endpoint: " + getEndpoint(COLLECTOR_OTLP_GRPC_PORT, OTLP_GRPC_TRACING_PATH));
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush();

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(grpcServer.traceRequests).hasSize(1));
    }
}
