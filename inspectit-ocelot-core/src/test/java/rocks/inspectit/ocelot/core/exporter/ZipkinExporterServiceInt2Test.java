package rocks.inspectit.ocelot.core.exporter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link ZipkinExporterService} using the {@link ExporterServiceIntegrationTestBase}
 */
public class ZipkinExporterServiceInt2Test extends ExporterServiceIntegrationTestBase {

    private static final String ZIPKIN_PATH = "/api/v2/spans";

    @Autowired
    ZipkinExporterService service;

    @Test
    void verifyTraceSent() {
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.tracing.zipkin.enabled", true);
            mps.setProperty("inspectit.exporters.tracing.zipkin.url", getEndpoint(COLLECTOR_ZIPKIN_PORT, ZIPKIN_PATH));
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush("zipkin-parent", "zipkin-child");

        awaitSpansExported("zipkin-parent", "zipkin-child");

    }

}


