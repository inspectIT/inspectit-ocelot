package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test for {@link JaegerExporterService} using the {@link ExporterServiceIntegrationTestBase}
 */
@DirtiesContext
public class JaegerThriftHttpExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static final String JAEGER_PATH = "/api/traces";

    @Autowired
    JaegerExporterService service;

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create().captureForType(JaegerExporterService.class, org.slf4j.event.Level.WARN);

    @DirtiesContext
    @Test
    void verifyTraceSent() {

        // enable Jaeger Thrift exporter
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.tracing.jaeger.url", getEndpoint(COLLECTOR_JAEGER_THRIFT_HTTP_PORT, JAEGER_PATH));
        });
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush("jaeger-thrift-parent", "jaeger-thrift-child");

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(grpcServer.traceRequests).hasSize(1));

    }

    @DirtiesContext
    @Test
    void testNoUrlSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.jaeger.url", "");
            props.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'url'");
    }

}
