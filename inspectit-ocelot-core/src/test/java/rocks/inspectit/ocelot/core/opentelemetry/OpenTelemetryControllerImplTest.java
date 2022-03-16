package rocks.inspectit.ocelot.core.opentelemetry;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.core.SLF4JBridgeHandlerUtils;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableMetricsExporterService;
import rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableTraceExporterService;
import rocks.inspectit.ocelot.core.exporter.LoggingTraceExporterService;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link OpenTelemetryControllerImpl}
 */
@ExtendWith(MockitoExtension.class)
class OpenTelemetryControllerImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryControllerImplTest.class);

    @Spy
    OpenTelemetryControllerImpl openTelemetryController = new OpenTelemetryControllerImpl();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitEnvironment env;

    @BeforeEach
    void initOpenTelemetryController() {
        // mock max-export-batch-size to avoid exceptions
        when(env.getCurrentConfig().getTracing().getMaxExportBatchSize()).thenReturn(512);
        openTelemetryController.env = env;
        openTelemetryController.init();
        openTelemetryController.start();
        clearInvocations(openTelemetryController);
    }

    @Test
    void testShutdown() {
        openTelemetryController.shutdown();
        assertThat(openTelemetryController.isStopped());
    }

    @Nested
    class ConfigureTracing {

        @Spy
        DynamicMultiSpanExporter spanExporter;

        @Spy
        TestTraceExporterService testTraceExporterService;

        @BeforeEach
        protected void init() {
            openTelemetryController.setSpanExporter(spanExporter);
            clearInvocations(openTelemetryController);
        }

        /**
         * Registers the {@link #testTraceExporterService} to the {@link #openTelemetryController} and verifies the interactions
         *
         * @param expected Whether the registration is expected to succeed.
         */
        protected void registerTestTraceExporterServiceAndVerify(boolean expected) {
            int registeredServices = openTelemetryController.registeredTraceExportServices.size();
            assertThat(openTelemetryController.registerTraceExporterService(testTraceExporterService)).isEqualTo(expected);
            assertThat(openTelemetryController.registeredTraceExportServices.size()).isEqualTo(registeredServices + (expected ? 1 : 0));
            verify(openTelemetryController, times(1)).registerTraceExporterService(testTraceExporterService);
            verify(openTelemetryController, times(expected ? 1 : 0)).notifyTracingSettingsChanged();
            verify(spanExporter, times(1)).registerSpanExporter(testTraceExporterService.getName(), testTraceExporterService.getSpanExporter());
            verifyNoMoreInteractions(openTelemetryController, spanExporter);
            clearInvocations(openTelemetryController, spanExporter);
        }

        /**
         * Unregisters the {@link #testTraceExporterService} from the {@link #openTelemetryController} and verifies the interactions
         *
         * @param expected Whether the unregistration is expected to succeed.
         */
        protected void unregisterTestTraceExporterServiceAndVerify(boolean expected) {
            int registeredServices = openTelemetryController.registeredTraceExportServices.size();
            assertThat(openTelemetryController.unregisterTraceExporterService(testTraceExporterService)).isEqualTo(expected);
            assertThat(openTelemetryController.registeredTraceExportServices.size()).isEqualTo(registeredServices - (expected ? 1 : 0));
            verify(openTelemetryController, times(1)).unregisterTraceExporterService(testTraceExporterService);
            verify(openTelemetryController, times(expected ? 1 : 0)).notifyTracingSettingsChanged();
            verify(spanExporter, times(1)).unregisterSpanExporter(testTraceExporterService.getName());
            verifyNoMoreInteractions(openTelemetryController, spanExporter);
            clearInvocations(openTelemetryController, spanExporter);
        }

        @DirtiesContext
        @Test
        void testRegisterTraceExporterService() {

            // check that service is successfully registered
            registerTestTraceExporterServiceAndVerify(true);

            // service cannot be registered twice
            registerTestTraceExporterServiceAndVerify(false);
        }

        @Test
        void testUnregisterTraceExporterService() {
            // when a service is not registered, unregistration fails
            unregisterTestTraceExporterServiceAndVerify(false);

            LOGGER.info(testTraceExporterService.getName() + " - " + openTelemetryController.registeredTraceExportServices.size());
            // register service
            registerTestTraceExporterServiceAndVerify(true);
            LOGGER.info("num of registered services:{}", openTelemetryController.registeredTraceExportServices.size());
            // unregistering should now succeed
            unregisterTestTraceExporterServiceAndVerify(true);
        }

        @Test
        void testConfigureTracing() {
            TracerProvider globalTracerProvider = GlobalOpenTelemetry.getTracerProvider();
            MeterProvider globalMeterProvider = GlobalOpenTelemetry.getMeterProvider();
            // register test service
            registerTestTraceExporterServiceAndVerify(true);
            // configure OTEL
            assertThat(openTelemetryController.configureOpenTelemetry()).isTrue();
            // tracing should have changed but metrics not
            verify(openTelemetryController, times(1)).configureTracing(any(InspectitConfig.class));
            verify(openTelemetryController, times(0)).configureMeterProvider(any(InspectitConfig.class));

            // verify that the tracer provider does not change after tracing has been (re-)configured
            assertThat(globalTracerProvider).isSameAs(GlobalOpenTelemetry.getTracerProvider());
            assertThat(globalMeterProvider).isSameAs(GlobalOpenTelemetry.getMeterProvider());
        }

        @Test
        void testShutdown() {
            openTelemetryController.shutdown();
            assertThat(OpenTelemetry.noop() == GlobalOpenTelemetry.get());
        }

        /**
         * A noop {@link DynamicallyActivatableTraceExporterService} for testing
         */
        class TestTraceExporterService extends DynamicallyActivatableTraceExporterService {

            @Override
            public SpanExporter getSpanExporter() {
                try {
                    Method m = Class.forName("io.opentelemetry.sdk.trace.export.NoopSpanExporter")
                            .getDeclaredMethod("getInstance");
                    m.setAccessible(true);
                    return (SpanExporter) m.invoke(null);
                } catch (Throwable t) {
                    t.printStackTrace();
                    return null;
                }
            }

            @Override
            protected boolean checkEnabledForConfig(InspectitConfig configuration) {
                return false;
            }

            @Override
            protected boolean doEnable(InspectitConfig configuration) {
                return true;
            }

            @Override
            protected boolean doDisable() {
                return true;
            }

            @Override
            public String getName() {
                return getClass().getSimpleName();
            }
        }
    }

    @Nested
    class ConfigureMeterProvider {

        @Spy
        TestMetricsExporterService testMetricsExporterService;

        /**
         * Registers the {@link #testMetricsExporterService} to the {@link #openTelemetryController} and verifies the interactions
         *
         * @param expected Whether the registration is expected to succeed.
         */
        private void registerTestMetricExporterServiceAndVerify(boolean expected) {
            int numRegisteredServices = openTelemetryController.registeredMetricExporterServices.size();
            assertThat(openTelemetryController.registerMetricExporterService(testMetricsExporterService)).isEqualTo(expected);
            assertThat(openTelemetryController.registeredMetricExporterServices.size()).isEqualTo(numRegisteredServices + (expected ? 1 : 0));
            verify(openTelemetryController, times(1)).registerMetricExporterService(testMetricsExporterService);
            verify(openTelemetryController, times(expected ? 1 : 0)).notifyMetricsSettingsChanged();
            clearInvocations(openTelemetryController);
        }

        /**
         * Unregisters the {@link #testMetricsExporterService} from the {@link #openTelemetryController} and verifies the interactions
         *
         * @param expected Whether the unregistration is expected to succeed.
         */
        private void unregisterTestMetricExporterServiceAndVerify(boolean expected) {
            int numRegisteredServices = openTelemetryController.registeredMetricExporterServices.size();
            assertThat(openTelemetryController.unregisterMetricExporterService(testMetricsExporterService)).isEqualTo(expected);
            assertThat(openTelemetryController.registeredMetricExporterServices.size()).isEqualTo(numRegisteredServices - (expected ? 1 : 0));
            verify(openTelemetryController, times(1)).unregisterMetricExporterService(testMetricsExporterService);
            verify(openTelemetryController, times(expected ? 1 : 0)).notifyMetricsSettingsChanged();
            clearInvocations(openTelemetryController);
        }

        @Test
        void testRegisterMetricsExporterService() {
            // first registration succeeds
            registerTestMetricExporterServiceAndVerify(true);

            // second should fail as the service was already registered
            registerTestMetricExporterServiceAndVerify(false);
        }

        @Test
        void testUnregisterMetricsExporterService() {
            // when a service is not registered, unregistration fails
            unregisterTestMetricExporterServiceAndVerify(false);

            // register service
            registerTestMetricExporterServiceAndVerify(true);

            // unregistration should succeed
            unregisterTestMetricExporterServiceAndVerify(true);
        }

        @Test
        void testConfigureMetrics() {
            MeterProvider globalMeterProvider = GlobalOpenTelemetry.getMeterProvider();
            TracerProvider globalTracerProvider = GlobalOpenTelemetry.getTracerProvider();

            SdkMeterProvider meterProvider = openTelemetryController.getMeterProvider();
            SdkTracerProvider tracerProvider = openTelemetryController.getTracerProvider();

            // register some service
            registerTestMetricExporterServiceAndVerify(true);
            // configure OTEL
            assertThat(openTelemetryController.configureOpenTelemetry()).isTrue();

            // verify that meter provider was configured but not tracing
            verify(openTelemetryController, times(1)).configureMeterProvider(any(InspectitConfig.class));
            verify(openTelemetryController, times(0)).configureTracing(any(InspectitConfig.class));

            // the meter provider should have changed, but the tracer provider not
            assertThat(openTelemetryController.getMeterProvider()).isNotSameAs(meterProvider);
            assertThat(globalMeterProvider).isNotSameAs(GlobalOpenTelemetry.getMeterProvider());
            assertThat(openTelemetryController.getTracerProvider()).isSameAs(tracerProvider);
            // the global tracer provider should have changed as we have rebuilt the OpenTelemetrySdk, which then creates a new ObfuscatedTracerProvider
            assertThat(globalTracerProvider).isNotSameAs(GlobalOpenTelemetry.getTracerProvider());
        }

        @Test
        void testShutdown() {
            openTelemetryController.shutdown();
            assertThat(null == GlobalOpenTelemetry.getMeterProvider());
        }

        /**
         * A noop {@link DynamicallyActivatableMetricsExporterService} for testing
         */
        class TestMetricsExporterService extends DynamicallyActivatableMetricsExporterService {

            @Mock
            MetricExporter metricExporter;

            @Override
            public MetricReaderFactory getNewMetricReaderFactory() {
                return PeriodicMetricReader.newMetricReaderFactory(new LoggingMetricExporter());
            }

            @Override
            protected boolean checkEnabledForConfig(InspectitConfig configuration) {
                return false;
            }

            @Override
            protected boolean doEnable(InspectitConfig configuration) {
                return false;
            }

            @Override
            protected boolean doDisable() {
                return false;
            }

            @Override
            public String getName() {
                return "test-metrics-exporter-service";
            }
        }
    }

    /**
     * Test changes in MetricsExporterSettings, which will lead to {@link SdkMeterProvider} being rebuilt and re-registered to {@link OpenTelemetryImpl}
     */
    @Nested
    static class ChangeMetrics extends SpringTestBase {

        @Autowired
        OpenTelemetryControllerImpl openTelemetryController;

        private static CloseableHttpClient testClient;

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

        @Test
        void testChangeMetricsExporterServices() throws Exception {

            SdkMeterProvider sdkMeterProvider = openTelemetryController.getMeterProvider();
            // enable prometheus and logging
            updateProperties(properties -> {
                properties.setProperty("inspectit.exporters.metrics.prometheus.enabled", ExporterEnabledState.ENABLED);
                properties.setProperty("inspectit.exporters.metrics.logging.enabled", true);
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

        @RegisterExtension
        LogCapturer spanLogs = LogCapturer.create().captureForType(LoggingSpanExporter.class);

        @Autowired
        LoggingTraceExporterService loggingTraceExporterService;

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
                properties.setProperty("inspectit.exporters.tracing.logging.enabled", true);
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
                properties.setProperty("inspectit.exporters.tracing.logging.enabled", false);
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

}