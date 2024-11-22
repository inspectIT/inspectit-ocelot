package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.trace.JaegerExporterSettings;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test class for the {@link JaegerExporterService}
 */
public class JaegerExporterServiceIntTest {

    static final int JAEGER_THRIFT_PORT = 14268;

    static final String JAEGER_THRIFT_PATH = "/api/traces";

    static final String JAEGER_GRPC_PATH = "/v1/traces";

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create().captureForType(JaegerExporterService.class, org.slf4j.event.Level.WARN);

    /**
     * Test for the {@link JaegerExporterService} using the {@link TransportProtocol#HTTP_THRIFT}
     */
    @DirtiesContext
    @Nested
    @TestPropertySource(properties = {"inspectit.exporters.tracing.jaeger.endpoint=http://localhost:14268/api/traces", "inspectit.exporters.tracing.jaeger.protocol=http/thrift", "inspectit.exporters.tracing.jaeger.enabled=ENABLED", "inspectit.tracing.max-export-batch-size=1"})
    class JaegerThriftExporterServiceIntTest extends SpringTestBase {

        private MockWebServer mockServer;

        @BeforeEach
        void setUpMockServer() throws IOException {
            mockServer = new MockWebServer();
            mockServer.start(JAEGER_THRIFT_PORT);
            MockResponse mockResponse = new MockResponse().setResponseCode(200);
            mockServer.enqueue(mockResponse);
        }

        @AfterEach
        void shutdownMockServer() throws IOException {
            mockServer.shutdown();
        }

        @Test
        void verifyTraceSent() {
            Span span = OpenTelemetryUtils.getTracer(Sampler.alwaysOn()).spanBuilder("zipkinspan").startSpan();
            try (Scope s = span.makeCurrent()) {
                System.out.println("dummy runnable");
            } finally {
                span.end();
            }

            await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
                Instances.openTelemetryController.flush();
                HttpUrl httpUrl = mockServer.takeRequest().getRequestUrl();
                assertThat(httpUrl).isNotNull();
                assertThat(httpUrl.url().getPath()).isEqualTo(JAEGER_THRIFT_PATH);
            });
        }
    }

    /**
     * Test for the {@link JaegerExporterService} using {@link ExporterServiceIntegrationTestBase}
     */
    @Nested
    @DirtiesContext
    class JaegerExporterServiceIntDockerTest extends ExporterServiceIntegrationTestBase {

        @Autowired
        JaegerExporterService service;

        @BeforeEach
        void clearRequests() {
            grpcServer.traceRequests.clear();
        }

        /**
         * Test using the {@link TransportProtocol#GRPC}
         */
        @Test
        void verifyTraceSentGrpc() {
            updateProperties(mps -> {
                mps.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.ENABLED);
                mps.setProperty("inspectit.exporters.tracing.jaeger.endpoint", getEndpoint(COLLECTOR_JAEGER_GRPC_PORT, JAEGER_GRPC_PATH));
                mps.setProperty("inspectit.exporters.tracing.jaeger.protocol", TransportProtocol.GRPC);
            });
            await().atMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

            makeSpansAndFlush("jaeger-grpc-parent", "jaeger-grpc-child");

            awaitSpansExported("jaeger-grpc-parent", "jaeger-grpc-child");
        }

        /**
         * Test using the {@link TransportProtocol#HTTP_THRIFT}
         */
        @Test
        void verifyTraceSentThrift() {
            updateProperties(mps -> {
                mps.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.ENABLED);
                mps.setProperty("inspectit.exporters.tracing.jaeger.endpoint", getEndpoint(COLLECTOR_JAEGER_THRIFT_HTTP_PORT, JAEGER_THRIFT_PATH));
                mps.setProperty("inspectit.exporters.tracing.jaeger.protocol", TransportProtocol.HTTP_THRIFT);
            });
            await().atMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

            makeSpansAndFlush("jaeger-thrift-parent", "jaeger-thrift-child");

            awaitSpansExported("jaeger-thrift-parent", "jaeger-thrift-child");
        }
    }

    /**
     * Tests for testing the {@link JaegerExporterSettings} in {@link rocks.inspectit.ocelot.config.model.exporters.trace.TraceExportersSettings#jaeger}
     */
    @DirtiesContext
    @Nested
    class JaegerSettingsIntTest extends SpringTestBase {

        @Autowired
        JaegerExporterService service;

        @Autowired
        InspectitEnvironment environment;

        @BeforeEach
        void beforeEach() {
            System.out.printf("jaeger endpoint=" + environment.getCurrentConfig()
                    .getExporters()
                    .getTracing()
                    .getJaeger()
                    .getEndpoint());
        }

        @DirtiesContext
        @Test
        void testEndpointNotSet() {
            updateProperties(props -> {
                props.setProperty("inspectit.exporters.tracing.jaeger.endpoint", "");
                props.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.ENABLED);
            });
            warnLogs.assertContains("'endpoint'");
        }

        @DirtiesContext
        @Test
        void testProtocolNotSet() {
            updateProperties(props -> {
                props.setProperty("inspectit.exporters.tracing.jaeger.protocol", "");
                props.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.ENABLED);
            });
            warnLogs.assertContains("protocol");
        }

        /**
         * Verifies the default {@link rocks.inspectit.ocelot.config.model.exporters.trace.JaegerExporterSettings settings} of the {@link JaegerExporterService}
         */
        @Test
        void defaultSettings() {
            JaegerExporterSettings jaeger = environment.getCurrentConfig().getExporters().getTracing().getJaeger();
            assertThat(service.isEnabled()).isFalse();
            assertThat(jaeger.getEnabled()).isEqualTo(ExporterEnabledState.IF_CONFIGURED);
            assertThat(jaeger.getEndpoint()).isNullOrEmpty();
            assertThat(jaeger.getProtocol()).isNull();
        }
    }
}
