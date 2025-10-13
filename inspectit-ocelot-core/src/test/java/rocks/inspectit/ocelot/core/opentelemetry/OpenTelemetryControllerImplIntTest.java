package rocks.inspectit.ocelot.core.opentelemetry;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.metrics.InstrumentValueType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.AggregationType;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.SLF4JBridgeHandlerUtils;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.exporter.LoggingTraceExporterService;
import rocks.inspectit.ocelot.core.opentelemetry.events.OpenTelemetryConfiguredEvent;
import rocks.inspectit.ocelot.core.testutils.TestEventListener;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.waitAtMost;

/**
 * Integration test class for the {@link OpenTelemetryControllerImpl}
 */
class OpenTelemetryControllerImplIntTest extends SpringTestBase {

    static CloseableHttpClient testClient;

    @Autowired
    OpenTelemetryControllerImpl openTelemetryController;

    @RegisterExtension
    LogCapturer spanLogs = LogCapturer.create().captureForType(LoggingSpanExporter.class);

    @Autowired
    LoggingTraceExporterService loggingTraceExporterService;

    @Autowired
    TestEventListener applicationEvents;

    @BeforeAll
    static void beforeAll() {
        SLF4JBridgeHandlerUtils.installSLF4JBridgeHandler();
    }

    @AfterAll
    static void afterAll() {
        SLF4JBridgeHandlerUtils.uninstallSLF4jBridgeHandler();
    }

    @BeforeAll
    static void initTestClient() {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        Timeout timeout = Timeout.of(1000, TimeUnit.MILLISECONDS);
        requestBuilder = requestBuilder.setConnectTimeout(timeout);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(timeout);

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        testClient = builder.build();
    }

    @AfterAll
    static void closeClient() throws Exception {
        testClient.close();
    }

    @Nested
    class Services {

        void assertGet200(String url) throws Exception {
            ClassicHttpRequest request = ClassicRequestBuilder.get().setUri(url).build();
            CloseableHttpResponse response = testClient.execute(request);
            int statusCode = response.getCode();
            assertThat(statusCode).isEqualTo(200);
            response.close();
        }

        void assertUnavailable(String url) {
            ClassicHttpRequest request = ClassicRequestBuilder.get().setUri(url).build();
            Throwable throwable = catchThrowable(() -> testClient.execute(request).getCode());

            assertThat(throwable).isInstanceOf(IOException.class);
        }

        /**
         * Test changes in MetricsExporterSettings, which will lead to {@link SdkMeterProvider} being rebuilt and re-registered to {@link OpenTelemetryImpl}
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
            await().atMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(newSdkMeterProvider).isNotSameAs(openTelemetryController.getMeterProvider()));

            // enable prometheus
            updateProperties(properties -> {
                properties.setProperty("inspectit.exporters.metrics.prometheus.enabled", ExporterEnabledState.ENABLED);
            });
            assertGet200("http://localhost:8888/metrics");

        }

        @Test
        void testChangeTracingExporterServices() throws InterruptedException {
            SdkTracerProvider sdkTracerProvider = openTelemetryController.getTracerProvider();
            // enable logging
            updateProperties(properties -> {
                properties.setProperty("inspectit.exporters.tracing.logging.enabled", ExporterEnabledState.ENABLED);
            });
            assertThat(loggingTraceExporterService.isEnabled()).isTrue();

            makeOtelSpansAndFlush("test-span");
            // verify the spans are logged
            waitAtMost(5, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(spanLogs.getEvents()).hasSize(1));
            assertThat(sdkTracerProvider).isEqualTo(openTelemetryController.getTracerProvider());

            // shut off tracer
            updateProperties(properties -> {
                properties.setProperty("inspectit.exporters.tracing.logging.enabled", ExporterEnabledState.DISABLED);
            });
            assertThat(loggingTraceExporterService.isEnabled()).isFalse();

            makeOtelSpansAndFlush("ignored-span");

            // verify that no more spans are logged
            Thread.sleep(5000);
            assertThat(spanLogs.getEvents()).hasSize(1);
        }
    }

    @Nested
    class ViewsChanged {

        static final String metricName = "my_metric";

        static final String viewName = "my_view";

        @Test
        void shouldUpdateWhenViewUpdated() {
            int startCount = applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class).size();

            MetricDefinitionSettings metric1 = createMetric();

            // create metric view
            updateProperties(properties ->
                properties.setProperty("inspectit.metrics.definitions." + metricName, metric1)
            );

            waitAtMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class))
                            .hasSize(startCount + 1)
                    );

            // update view
            MetricDefinitionSettings metric2 = createMetric(AggregationType.HISTOGRAM);
            updateProperties(properties ->
                properties.setProperty("inspectit.metrics.definitions." + metricName, metric2)
            );

            waitAtMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class))
                            .hasSize(startCount + 2)
                    );
        }

        @Test
        void shouldUpdateWhenViewDisabled() {
            int startCount = applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class).size();

            MetricDefinitionSettings metric1 = createMetric();

            // create metric view
            updateProperties(properties ->
                properties.setProperty("inspectit.metrics.definitions." + metricName, metric1)
            );

            waitAtMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class))
                            .hasSize(startCount + 1)
                    );

            // disable view
            MetricDefinitionSettings metric2 = createMetric(false);
            updateProperties(properties ->
                properties.setProperty("inspectit.metrics.definitions." + metricName, metric2)
            );

            waitAtMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class))
                            .hasSize(startCount + 2)
                    );
        }

        @Test
        void shouldUpdateWhenMetricDisabled() {
            int startCount = applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class).size();

            MetricDefinitionSettings metric1 = createMetric();

            // create metric view
            updateProperties(properties ->
                properties.setProperty("inspectit.metrics.definitions." + metricName, metric1)
            );

            waitAtMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class))
                            .hasSize(startCount + 1)
                    );

            // disable view
            MetricDefinitionSettings metric2 = createMetric();
            metric2.setEnabled(false);
            updateProperties(properties ->
                properties.setProperty("inspectit.metrics.definitions." + metricName, metric2)
            );

            waitAtMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class))
                            .hasSize(startCount + 2)
                    );
        }

        @Test
        void shouldNotUpdateWhenOnlyMetricUpdated() {
            int startCount = applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class).size();

            MetricDefinitionSettings metric1 = createMetric();

            // create metric view
            updateProperties(properties ->
                properties.setProperty("inspectit.metrics.definitions." + metricName, metric1)
            );


            waitAtMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class))
                            .hasSize(startCount + 1)
                    );

            // disable metric
            MetricDefinitionSettings metric2 = createMetric();
            metric2.setValueType(InstrumentValueType.DOUBLE); // default is LONG
            updateProperties(properties ->
                properties.setProperty("inspectit.metrics.definitions." + metricName, metric2)
            );

            waitAtMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(applicationEvents.getEvents(OpenTelemetryConfiguredEvent.class))
                            .anyMatch(event -> !event.isUpdateMetrics())
                    );
        }

        private MetricDefinitionSettings createMetric() {
            return createMetric(true, AggregationType.LAST_VALUE);
        }

        private MetricDefinitionSettings createMetric(AggregationType aggregation) {
            return createMetric(true, aggregation);
        }

        private MetricDefinitionSettings createMetric(boolean viewEnabled) {
            return createMetric(viewEnabled, AggregationType.LAST_VALUE);
        }

        private MetricDefinitionSettings createMetric(boolean viewEnabled, AggregationType aggregation) {
            MetricDefinitionSettings metric = new MetricDefinitionSettings();
            metric.setUnit("unit");
            ViewDefinitionSettings view = new ViewDefinitionSettings();
            view.setEnabled(viewEnabled);
            view.setAggregation(aggregation);
            metric.setViews(singletonMap(viewName, view));

            return metric;
        }
    }

    private static void makeOtelSpansAndFlush(String spanName) {
        // build and flush span
        Span span = GlobalOpenTelemetry.getTracerProvider()
                .get("rocks.inspectit.instrumentation.test")
                .spanBuilder(spanName)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {}
        finally {
            span.end();
        }
        OpenTelemetryUtils.flush();
    }
}
