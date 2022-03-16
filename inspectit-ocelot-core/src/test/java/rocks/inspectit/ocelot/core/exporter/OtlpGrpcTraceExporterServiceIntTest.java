package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.metrics.OtlpGrpcMetricsExporterSettings;
import rocks.inspectit.ocelot.config.model.exporters.trace.OtlpGrpcTraceExporterSettings;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test for the {@link OtlpGrpcTraceExporterService}
 */
@DirtiesContext
public class OtlpGrpcTraceExporterServiceIntTest extends ExporterServiceIntegrationTestBase {

    public static final String OTLP_GRPC_TRACING_PATH = "/v1/trace";

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create().captureForType(OtlpGrpcTraceExporterService.class, org.slf4j.event.Level.WARN);

    @Autowired
    private OtlpGrpcTraceExporterService service;

    @DirtiesContext
    @Test
    void verifyTraceSent() {
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.tracing.otlp-grpc.url", getEndpoint(COLLECTOR_OTLP_GRPC_PORT, OTLP_GRPC_TRACING_PATH));
            properties.setProperty("inspectit.exporters.tracing.otlp-grpc.enabled", ExporterEnabledState.ENABLED);
        });

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        makeSpansAndFlush("otlp-grpc-parent", "otlp-grpc-child");

        awaitSpansExported("otlp-grpc-parent", "otlp-grpc-child");
    }

    @DirtiesContext
    @Test
    void testNoUrlSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.otlp-grpc.url", "");
            props.setProperty("inspectit.exporters.tracing.otlp-grpc.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'url'");
    }

    @Test
    void defaultSettings() {
        // service is not running
        AssertionsForClassTypes.assertThat(service.isEnabled()).isFalse();

        OtlpGrpcTraceExporterSettings otlpGrpc = environment.getCurrentConfig()
                .getExporters()
                .getTracing()
                .getOtlpGrpc();
        // enabled property is set to IF_CONFIGURED
        AssertionsForClassTypes.assertThat(otlpGrpc.getEnabled().equals(ExporterEnabledState.IF_CONFIGURED));
        // url is null or empty
        AssertionsForClassTypes.assertThat(otlpGrpc.getUrl()).isNullOrEmpty();
    }

}
