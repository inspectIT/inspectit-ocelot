package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.trace.OtlpTraceExporterSettings;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test for the {@link OtlpTraceExporterService}
 */
@DirtiesContext
public class OtlpTraceExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static final String OTLP_GRPC_TRACING_PATH = "/v1/trace";

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
            properties.setProperty("inspectit.exporters.tracing.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT, OTLP_GRPC_TRACING_PATH));
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
            properties.setProperty("inspectit.exporters.tracing.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_HTTP_PORT, OTLP_GRPC_TRACING_PATH));
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
        assertThat(otlp.getProtocol()).isEqualTo(TransportProtocol.UNSET);
    }

}
