package rocks.inspectit.ocelot.core.exporter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DirtiesContext
public class JaegerThriftHttpExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static final String JAEGER_PATH = "/api/traces";

    @Autowired
    JaegerExporterService service;

    @DirtiesContext
    @Test
    void verifyTraceSent() {

        // enable Jaeger Thrift exporter
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.tracing.jaeger.enabled", true);
            mps.setProperty("inspectit.exporters.tracing.jaeger.url", getEndpoint(COLLECTOR_JAEGER_THRIFT_HTTP_PORT, JAEGER_PATH));
        });
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush();

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(grpcServer.traceRequests).hasSize(1));

    }

}
