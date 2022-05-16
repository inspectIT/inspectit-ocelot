package rocks.inspectit.ocelot.core.opentelemetry;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.core.SLF4JBridgeHandlerUtils;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.exporter.LoggingTraceExporterService;
import rocks.inspectit.ocelot.core.opentelemetry.trace.CustomIdGenerator;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Integration test class for the {@link OpenTelemetryControllerImpl}
 */
public class OpenTelemetryControllerImplIntTest extends SpringTestBase {

    private static CloseableHttpClient testClient;

    @Autowired
    OpenTelemetryControllerImpl openTelemetryController;

    @RegisterExtension
    LogCapturer spanLogs = LogCapturer.create().captureForType(LoggingSpanExporter.class);

    @Autowired
    LoggingTraceExporterService loggingTraceExporterService;

    @BeforeAll
    static void beforeAll() {
        SLF4JBridgeHandlerUtils.installSLF4JBridgeHandler();
    }

    @AfterAll
    static void afterAll() {
        SLF4JBridgeHandlerUtils.uninstallSLF4jBridgeHandler();
    }

    @BeforeAll
    private static void initTestClient() {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(1000);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(1000);

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        testClient = builder.build();
    }

    @AfterAll
    static void closeClient() throws Exception {
        testClient.close();
    }

    void assertGet200(String url) throws Exception {
        CloseableHttpResponse response = testClient.execute(new HttpGet(url));
        int statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode).isEqualTo(200);
        response.close();
    }

    void assertUnavailable(String url) {
        Throwable throwable = catchThrowable(() -> testClient.execute(new HttpGet(url))
                .getStatusLine()
                .getStatusCode());

        assertThat(throwable).isInstanceOf(IOException.class);
    }

    /**
     * Test changes in MetricsExporterSettings, which will lead to {@link SdkMeterProvider} being rebuilt and re-registered to {@link OpenTelemetryImpl}
     *
     * @throws Exception
     */
    @Test
    void testChangeMetricsExporterServices() throws Exception {

        SdkMeterProvider sdkMeterProvider = openTelemetryController.getMeterProvider();
        // enable prometheus and logging
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.metrics.prometheus.enabled", ExporterEnabledState.ENABLED);
            properties.setProperty("inspectit.exporters.metrics.logging.enabled", ExporterEnabledState.ENABLED);
        });
        // wait until the OpenTelemetryController has been reconfigured
        SdkMeterProvider newSdkMeterProvider = openTelemetryController.getMeterProvider();
        // meter provider should have changed
        assertThat(sdkMeterProvider).isNotSameAs(newSdkMeterProvider);
        // Prometheus should be running
        assertGet200("http://localhost:8888/metrics");

        // disable prometheus
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.metrics.prometheus.enabled", ExporterEnabledState.DISABLED);
        });
        assertUnavailable("http://localhost:8888/metrics");

        // wait until the SdkMeterProvider has been rebuilt
        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(newSdkMeterProvider).isNotSameAs(openTelemetryController.getMeterProvider()));

        // enable prometheus
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.metrics.prometheus.enabled", ExporterEnabledState.ENABLED);
        });
        assertGet200("http://localhost:8888/metrics");

    }

    /**
     * Verify that the {@link io.opencensus.trace.Tracer} in {@link Tracing#getTracer()} is correctly set to {@link GlobalOpenTelemetry#getTracerProvider()}
     *
     * @throws InterruptedException
     */
    @Test
    void testChangeTracingExporterServices() throws InterruptedException {
        SdkTracerProvider sdkTracerProvider = openTelemetryController.getTracerProvider();
        // enable logging
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.tracing.logging.enabled", ExporterEnabledState.ENABLED);
        });
        assertThat(loggingTraceExporterService.isEnabled()).isTrue();
        // make OC spans and flush
        makeOCSpansAndFlush("test-span");
        // verify the spans are logged
        Awaitility.waitAtMost(5, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(spanLogs.getEvents()).hasSize(1));
        assertThat(sdkTracerProvider).isEqualTo(openTelemetryController.getTracerProvider());

        // shut off tracer
        updateProperties(properties -> {
            properties.setProperty("inspectit.exporters.tracing.logging.enabled", ExporterEnabledState.DISABLED);
        });
        assertThat(loggingTraceExporterService.isEnabled()).isFalse();
        // make OC spans and flush
        makeOCSpansAndFlush("ignored-span");
        // verify that no more spans are logged
        Thread.sleep(5000);
        assertThat(spanLogs.getEvents()).hasSize(1);
    }

    private static void makeOtelSpansAndFlush(String spanName) {
        // build and flush span
        Span span = GlobalOpenTelemetry.getTracerProvider()
                .get("rocks.inspectit.instrumentation.test")
                .spanBuilder(spanName)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
        } finally {
            span.end();
        }
        OpenTelemetryUtils.flush();
    }

    private static void makeOCSpansAndFlush(String spanName) {
        // get OC tracer and start spans
        io.opencensus.trace.Tracer tracer = Tracing.getTracer();

        // start span
        try (io.opencensus.common.Scope scope = tracer.spanBuilder(spanName).startScopedSpan()) {
            io.opencensus.trace.Span span = tracer.getCurrentSpan();
            span.addAnnotation("anno");
        }
        OpenTelemetryUtils.flush();
    }

}
