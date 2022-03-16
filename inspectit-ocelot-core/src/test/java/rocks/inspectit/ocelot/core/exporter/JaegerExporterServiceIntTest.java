package rocks.inspectit.ocelot.core.exporter;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.netmikey.logunit.api.LogCapturer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {"inspectit.exporters.tracing.jaeger.url=http://127.0.0.1:14268/api/traces", "inspectit.exporters.tracing.jaeger.grpc=http://127.0.0.1:14267/api/traces", "inspectit.tracing.max-export-batch-size=1"})
@DirtiesContext
public class JaegerExporterServiceIntTest extends SpringTestBase {

    private static final Logger logger = LoggerFactory.getLogger(JaegerExporterServiceIntTest.class);

    public static final int JAEGER_PORT = 14268;

    public static final String JAEGER_PATH = "/api/traces";

    private WireMockServer wireMockServer;

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create().captureForType(JaegerExporterService.class, org.slf4j.event.Level.WARN);

    @BeforeEach
    void setupWiremock() {
        wireMockServer = new WireMockServer(options().port(JAEGER_PORT));
        wireMockServer.start();
        configureFor(wireMockServer.port());

        stubFor(post(urlPathEqualTo(JAEGER_PATH)).willReturn(aResponse().withStatus(200)));
    }

    @AfterEach
    void cleanup() {
        wireMockServer.stop();
    }

    @Test
    void verifyTraceSent() throws InterruptedException {
        Tracing.getTracer().spanBuilder("jaegerspan").setSampler(Samplers.alwaysSample()).startSpanAndRun(() -> {
        });

        logger.info("Wait for Jaeger to process the span...");
        Thread.sleep(1100L);

        Instances.openTelemetryController.flush();

        await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postRequestedFor(urlPathEqualTo(JAEGER_PATH)));
        });
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
