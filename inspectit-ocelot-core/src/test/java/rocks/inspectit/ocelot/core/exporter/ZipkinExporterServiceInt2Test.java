package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.LoggingEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link ZipkinExporterService} using the {@link ExporterServiceIntegrationTestBase}
 */
public class ZipkinExporterServiceInt2Test extends ExporterServiceIntegrationTestBase {

    private static final String ZIPKIN_PATH = "/api/v2/spans";

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create()
            .captureForType(ZipkinExporterService.class, org.slf4j.event.Level.WARN);

    @Autowired
    ZipkinExporterService service;

    @DirtiesContext
    @Test
    void verifyTraceSent() {
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.tracing.zipkin.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.tracing.zipkin.url", getEndpoint(COLLECTOR_ZIPKIN_PORT, ZIPKIN_PATH));
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush("zipkin-parent", "zipkin-child");

        awaitSpansExported("zipkin-parent", "zipkin-child");

    }

    @DirtiesContext
    @Test
    void testNoUrlSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.zipkin.url", "");
            props.setProperty("inspectit.exporters.tracing.zipkin.enabled", ExporterEnabledState.ENABLED);
        });
        System.out.println("----- WARNING LOGS ----");
        for (LoggingEvent event : warnLogs.getEvents()) {
            System.out.println(event.getMessage());
        }
        warnLogs.assertContains("'url'");
    }

}


