package rocks.inspectit.ocelot.core.exporter;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {"inspectit.exporters.tracing.zipkin.endpoint=http://127.0.0.1:9411/api/v2/spans", "inspectit.tracing.max-export-batch-size=512", "inspectit.tracing.schedule-delay-millis=20000"})
@DirtiesContext
public class ZipkinExporterServiceIntTest extends SpringTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ZipkinExporterServiceIntTest.class);

    private static final int ZIPKIN_PORT = 9411;

    private static final String ZIPKIN_PATH = "/api/v2/spans";

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create().captureForType(ZipkinExporterService.class, org.slf4j.event.Level.WARN);

    private WireMockServer wireMockServer;

    @BeforeEach
    void setupWiremock() {
        wireMockServer = new WireMockServer(options().port(ZIPKIN_PORT));
        wireMockServer.start();
        configureFor(wireMockServer.port());

        stubFor(post(urlPathEqualTo(ZIPKIN_PATH)).willReturn(aResponse().withStatus(200)));
    }

    @AfterEach
    void cleanup() {
        wireMockServer.stop();
    }

    @Test
    void verifyTraceSent() {
        Span span = OpenTelemetryUtils.getTracer(Sampler.alwaysOn()).spanBuilder("zipkinspan").startSpan();
        try (Scope s = span.makeCurrent()) {
        } finally {
            span.end();
        }

        await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Instances.openTelemetryController.flush();
            verify(postRequestedFor(urlPathEqualTo(ZIPKIN_PATH)).withRequestBody(containing("zipkinspan")));
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
}
