package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.CompressionMethod;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.trace.OtlpTraceExporterSettings;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test for the {@link OtlpTraceExporterService}
 */
@DirtiesContext
public class OtlpTraceExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static final String OTLP_TRACING_PATH = "/v1/traces";

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create()
            .captureForType(OtlpTraceExporterService.class, org.slf4j.event.Level.WARN);

    @Autowired
    private OtlpTraceExporterService service;

    @BeforeEach
    void clearRequests() {
        grpcServer.traceRequests.clear();
    }

    @DirtiesContext
    @Test
    void verifyTraceSentGrpc() {
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.tracing.otlp.protocol", TransportProtocol.GRPC);
            properties.setProperty("inspectit.exporters.tracing.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            properties.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.ENABLED);
        });

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush("otlp-grpc-parent", "otlp-grpc-child");

        awaitSpansExported("otlp-grpc-parent", "otlp-grpc-child");
    }

    @DirtiesContext
    @Test
    void verifyTraceSentHttp() {
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.tracing.otlp.protocol", TransportProtocol.HTTP_PROTOBUF);
            properties.setProperty("inspectit.exporters.tracing.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_HTTP_PORT, OTLP_TRACING_PATH));
            properties.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.ENABLED);
        });

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush("otlp-http-parent", "otlp-http-child");

        awaitSpansExported("otlp-http-parent", "otlp-http-child");
    }

    @DirtiesContext
    @Test
    void testNoEndpointSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.otlp.endpoint", "");
            props.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'endpoint'");
    }

    @DirtiesContext
    @Test
    void testNoProtocolSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.otlp.protocol", "");
            props.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'protocol'");
    }

    @Test
    void defaultSettings() {
        AssertionsForClassTypes.assertThat(service.isEnabled()).isFalse();
        OtlpTraceExporterSettings otlp = environment.getCurrentConfig().getExporters().getTracing().getOtlp();
        assertThat(otlp.getEnabled().equals(ExporterEnabledState.IF_CONFIGURED));
        assertThat(otlp.getEndpoint()).isNullOrEmpty();
        assertThat(otlp.getProtocol()).isNull();
        assertThat(otlp.getHeaders()).isNullOrEmpty();
        assertThat(otlp.getCompression()).isEqualTo(CompressionMethod.NONE);
        assertThat(otlp.getTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @DirtiesContext
    @Test
    void testHeaders(){
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.tracing.otlp.protocol", TransportProtocol.HTTP_PROTOBUF);
            properties.setProperty("inspectit.exporters.tracing.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_HTTP_PORT, OTLP_TRACING_PATH));
            properties.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.ENABLED);
            properties.setProperty("inspectit.exporters.tracing.otlp.headers", new HashMap<String, String>(){{put("my-header-key","my-header-value");}});

        });
        assertThat(service.isEnabled()).isTrue();
        assertThat(environment.getCurrentConfig().getExporters().getTracing().getOtlp().getHeaders()).containsEntry("my-header-key","my-header-value");
    }

    @DirtiesContext
    @Test
    void testCompression() {
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.tracing.otlp.protocol", TransportProtocol.GRPC);
            properties.setProperty("inspectit.exporters.tracing.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_HTTP_PORT, OTLP_TRACING_PATH));
            properties.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.ENABLED);
            properties.setProperty("inspectit.exporters.tracing.otlp.compression", CompressionMethod.GZIP);
        });
        assertThat(service.isEnabled()).isTrue();
        assertThat(environment.getCurrentConfig().getExporters().getTracing().getOtlp().getCompression()).isEqualTo(CompressionMethod.GZIP);
    }
}
