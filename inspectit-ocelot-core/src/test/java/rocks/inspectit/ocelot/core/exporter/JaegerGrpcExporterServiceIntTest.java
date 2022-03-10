package rocks.inspectit.ocelot.core.exporter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class JaegerGrpcExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static String JAEGER_GRPC_PATH = "/v1/traces";

    @Autowired
    JaegerGrpcExporterService service;

    @Test
    @DirtiesContext
    void verifyTraceSent() {
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.tracing.jaeger-grpc.enabled", true);
            mps.setProperty("inspectit.exporters.tracing.jaeger-grpc.grpc", getEndpoint(COLLECTOR_JAEGER_GRPC_PORT, JAEGER_GRPC_PATH));
        });
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush("jaeger-grpc-parent","jaeger-grpc-child");

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(grpcServer.traceRequests).hasSize(1));
    }
}
