package rocks.inspectit.ocelot.core.exporter;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {"inspectit.exporters.tracing.logging.enabled=true", "inspectit.tracing.max-export-batch-size=1"})
@DirtiesContext
public class OtlpGrpcTraceExporterServiceIntTest extends SpringTestBase {

    private static final Logger logger = LoggerFactory.getLogger(OtlpGrpcTraceExporterService.class);

    @RegisterExtension
    LogCapturer spanLogs = LogCapturer.create().captureForType(LoggingSpanExporter.class);

    public static final int OTLP_GRPC_PORT = 4318;

    public static final String OTLP_GRPC_PATH = "/v1/trace";

    private WireMockServer wireMockServer;

    @Autowired
    private OtlpGrpcTraceExporterService service;

    @DirtiesContext
    @BeforeEach
    void enableService() {
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.tracing.otlpGrpc.url", String.format("http://127.0.0.1:%d%s", OTLP_GRPC_PORT, OTLP_GRPC_PATH));
            properties.setProperty("inspectit.exporters.tracing.otlpGrpc.enabled", true);
        });
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());
    }

    @BeforeEach
    void setupWiremock() {
        wireMockServer = new WireMockServer(options().port(OTLP_GRPC_PORT));
        wireMockServer.start();
        configureFor(wireMockServer.port());

        stubFor(post(urlPathEqualTo(OTLP_GRPC_PATH)).willReturn(aResponse().withStatus(200)));
    }

    @AfterEach
    void cleanup() {
        wireMockServer.stop();
    }

    @Test
    void verifyTraceSent() throws InterruptedException {
        Span span = GlobalOpenTelemetry.getTracer("rocks.inspectit.instrumentation", "0.0.1")
                .spanBuilder("otlp-grpc-span")
                .startSpan();
        try (Scope sc = span.makeCurrent()) {
        } finally {
            span.end();
        }
        Instances.openTelemetryController.flush();

        logger.info("Wait for OTLP to process the span...");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(spanLogs.size()).isGreaterThan(0));
        Thread.sleep(1100L);

        await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postRequestedFor(urlPathEqualTo(OTLP_GRPC_PATH)));
        });
    }
}
