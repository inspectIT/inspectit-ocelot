package rocks.inspectit.ocelot.core.exporter;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opencensus.exporter.trace.jaeger.JaegerTraceExporter;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {
        "inspectit.exporters.tracing.jaeger.url=http://127.0.0.1:14268/api/traces"
})
@DirtiesContext
public class JaegerExporterServiceIntTest extends SpringTestBase {

    private static final Logger logger = LoggerFactory.getLogger(JaegerExporterServiceIntTest.class);

    public static final int JAEGER_PORT = 14268;
    public static final String JAEGER_PATH = "/api/traces";
    private WireMockServer wireMockServer;

    @BeforeEach
    void setupWiremock() {
        wireMockServer = new WireMockServer(options().port(JAEGER_PORT));
        wireMockServer.start();
        configureFor(wireMockServer.port());
        stubFor(get(urlPathEqualTo(JAEGER_PATH))
                .willReturn(aResponse()
                        .withStatus(200)));

    }

    @AfterEach
    void cleanup() {
        wireMockServer.stop();
    }

    @Test
    void verifyTraceSent() throws InterruptedException {
        Tracing.getTracer().spanBuilder("jaegerspan")
                .setSampler(Samplers.alwaysSample())
                .startSpanAndRun(() -> {
                });

        // Shutdown the export component to force a flush. This will cause problems if multiple tests
        // are added in this class, but this is not the case for the moment.
        Tracing.getExportComponent().shutdown();
        JaegerTraceExporter.unregister();

        logger.info("Wait for Jaeger to process the span...");
        long timeWaitingForSpansToBeExportedInMillis = 1100L;
        Thread.sleep(timeWaitingForSpansToBeExportedInMillis);

        await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postRequestedFor(urlPathEqualTo(JAEGER_PATH)));
        });
    }

}
