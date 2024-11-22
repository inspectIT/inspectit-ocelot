package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.trace.ZipkinExporterSettings;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {"inspectit.exporters.tracing.zipkin.endpoint=http://127.0.0.1:9411/api/v2/spans", "inspectit.tracing.max-export-batch-size=512", "inspectit.tracing.schedule-delay-millis=20000"})
@DirtiesContext
public class ZipkinExporterServiceIntTest extends SpringTestBase {

    private static final int ZIPKIN_PORT = 9411;

    private static final String ZIPKIN_PATH = "/api/v2/spans";

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create().captureForType(ZipkinExporterService.class, org.slf4j.event.Level.WARN);

    private MockWebServer mockServer;

    @BeforeEach
    void setupWiremock() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start(ZIPKIN_PORT);
        MockResponse mockResponse = new MockResponse().setResponseCode(200);
        mockServer.enqueue(mockResponse);
    }

    @AfterEach
    void shutdown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void verifyTraceSent() {
        Span span = OpenTelemetryUtils.getTracer().spanBuilder("zipkinspan").startSpan();
        try (Scope s = span.makeCurrent()) {
        } finally {
            span.end();
        }

        await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Instances.openTelemetryController.flush();
            Buffer body = mockServer.takeRequest().getBody();
            assertCompressedContent(body, "zipkinspan");
        });
    }

    @DirtiesContext
    @Test
    void testNoEndpointSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.zipkin.endpoint", "");
            props.setProperty("inspectit.exporters.tracing.zipkin.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'endpoint'");
    }

    @Nested
    class ZipkinExporterSettingsIntTest {

        /**
         * Remove all {@link PropertySource} that are not part of the original inspectIT environment
         */
        @BeforeEach
        void resetPropertySources() {
            environment.updatePropertySources(propertySources -> {
                for (PropertySource<?> prop : environment.getPropertySources()) {
                    if (!prop.getName().toLowerCase().contains("inspectit")) {
                        propertySources.remove(prop.getName());
                    }
                }
            });
        }

        @Autowired
        InspectitEnvironment environment;

        @Autowired
        ZipkinExporterService service;

        /**
         * Verifies the default {@link rocks.inspectit.ocelot.config.model.exporters.trace.ZipkinExporterSettings settings}
         */
        @Test
        void defaultSettings() {
            ZipkinExporterSettings zipkin = environment.getCurrentConfig().getExporters().getTracing().getZipkin();
            assertThat(service.isEnabled()).isFalse();
            assertThat(zipkin.getEnabled()).isEqualTo(ExporterEnabledState.IF_CONFIGURED);
            assertThat(zipkin.getEndpoint()).isNullOrEmpty();
        }
    }

    /**
     * Asserts that the buffer contains the provided content. We need this helper method, since
     * the body of the Zipkin exporter request is compressed.
     * Fails if an exception was thrown.
     *
     * @param buffer the buffer with data
     * @param content the content, which should be included
     */
    private void assertCompressedContent(Buffer buffer, String content) {
        byte[] compressedBytes = buffer.readByteArray();
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressedBytes));
            InputStreamReader isr = new InputStreamReader(gis);
            BufferedReader br = new BufferedReader(isr)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                assertThat(sb.toString()).contains(content);
        } catch (IOException e) {
            fail("Could not decompress data: " + e.getMessage());
        }
    }
}
