package rocks.inspectit.ocelot.core.metrics;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.exporter.ExporterServiceIntegrationTestBase;
import rocks.inspectit.ocelot.core.exporter.OtlpMetricsExporterService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration Test Class for {@link MeasureTagValueGuard}
 */
public class MeasureTagValueGuardIntTest extends ExporterServiceIntegrationTestBase {
    public static final String OTLP_METRICS_PATH = "/v1/metrics";


    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create()
            .captureForType(OtlpMetricsExporterService.class, org.slf4j.event.Level.WARN);

    @Autowired
    OtlpMetricsExporterService service;

    @Autowired
    InspectitEnvironment environment;

    static final String TAG_KEY = "tag-value-guard-test-tag-key";

    @BeforeEach
    void clearRequests() {
        getGrpcServer().getMetricRequests().clear();
    }

    /**
     * TODO: Update test
     * Since recordMetricsAndFlush() creates metrics directly in OpenTelemetry, MeasureTagValueGuard cannot be applied
     * Thus to test MeasureTagValueGuard, metrics have to be created with InspectIT
     */
    @DirtiesContext
    @Test
    @Disabled("Metrics need to be created with InspectIT")
    void testValueGuardLimitExceed() throws InterruptedException {
        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            mps.setProperty("inspectit.exporters.metrics.otlp.export-interval", "500ms");
            mps.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
            mps.setProperty("inspectit.metrics.tag-guard.max-tag-values-per-tag", 2);
            mps.setProperty("inspectit.metrics.tag-guard.schedule-delay", Duration.ofMillis(500));
        });

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

        recordMetricsAndFlush(1, TAG_KEY, "1");
        recordMetricsAndFlush(2, TAG_KEY, "2");

        awaitMetricsExported(1, TAG_KEY, "1");
        awaitMetricsExported(2, TAG_KEY, "2");

        Thread.sleep(1000);
        recordMetricsAndFlush(3, TAG_KEY, "3");
        awaitMetricsExported(3, TAG_KEY, "TAG_LIMIT_EXCEEDED");

    }


}
