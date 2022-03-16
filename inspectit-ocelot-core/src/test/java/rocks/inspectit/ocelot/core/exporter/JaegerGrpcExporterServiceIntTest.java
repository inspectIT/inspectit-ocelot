package rocks.inspectit.ocelot.core.exporter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

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
            mps.setProperty("inspectit.exporters.tracing.jaeger-grpc.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.tracing.jaeger-grpc.grpc", getEndpoint(COLLECTOR_JAEGER_GRPC_PORT, JAEGER_GRPC_PATH));
        });
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush("jaeger-grpc-parent", "jaeger-grpc-child");

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(grpcServer.traceRequests).hasSize(1));
    }

    /**
     * Test the default {@link rocks.inspectit.ocelot.config.model.exporters.trace.JaegerGrpcExporterSettings settings} of the {@link JaegerGrpcExporterService}
     */
    @Test
    void testDefault() {
        // service should be disabled
        assertThat(service.isEnabled()).isFalse();
        // enabled flag should be IF_CONFIGURED
        assertThat(environment.getCurrentConfig()
                .getExporters()
                .getTracing()
                .getJaegerGrpc()
                .getEnabled()).isEqualTo(ExporterEnabledState.IF_CONFIGURED);
        // gRPC API endpoint should be null or empty
        assertThat(environment.getCurrentConfig().getExporters().getTracing().getJaegerGrpc().getGrpc()).isNullOrEmpty();
    }
}
