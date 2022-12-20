package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableMetricsExporterService;
import rocks.inspectit.ocelot.core.opentelemetry.trace.CustomIdGenerator;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test class for unit tests of {@link OpenTelemetryControllerImpl}
 */
@ExtendWith(MockitoExtension.class)
class OpenTelemetryControllerImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryControllerImplTest.class);

    @Spy
    OpenTelemetryControllerImpl openTelemetryController = new OpenTelemetryControllerImpl();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitEnvironment env;

    @Mock
    CustomIdGenerator idGenerator;

    @BeforeEach
    void initOpenTelemetryController() {
        // mock max-export-batch-size to avoid exceptions
        when(env.getCurrentConfig().getTracing().getMaxExportBatchSize()).thenReturn(512);
        openTelemetryController.env = env;
        openTelemetryController.idGenerator = idGenerator;
        openTelemetryController.init();
        openTelemetryController.start();
        clearInvocations(openTelemetryController);
    }

    @Test
    void testShutdown() {
        openTelemetryController.shutdown();
        assertThat(openTelemetryController.isShutdown());
    }

    @Nested
    class ConfigureTracing {

        @Spy
        DynamicMultiSpanExporter spanExporter;

        @Spy
        TestTraceExporterService testTraceExporterService;

        @BeforeEach
        protected void init() {
            openTelemetryController.setMultiSpanExporter(spanExporter);
            clearInvocations(openTelemetryController);
        }

        /**
         * Registers the {@link #testTraceExporterService} to the {@link #openTelemetryController} and verifies the interactions
         *
         * @param expected Whether the registration is expected to succeed.
         */
        protected void registerTestTraceExporterServiceAndVerify(boolean expected) {
            int registeredServices = openTelemetryController.registeredTraceExportServices.size();
            assertThat(openTelemetryController.registerTraceExporterService(testTraceExporterService.getSpanExporter(), testTraceExporterService.getName())).isEqualTo(expected);
            assertThat(openTelemetryController.registeredTraceExportServices.size()).isEqualTo(registeredServices + (expected ? 1 : 0));
            verify(openTelemetryController, times(1)).registerTraceExporterService(testTraceExporterService.getSpanExporter(), testTraceExporterService.getName());
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
            assertThat(openTelemetryController.unregisterTraceExporterService(testTraceExporterService.getName())).isEqualTo(expected);
            assertThat(openTelemetryController.registeredTraceExportServices.size()).isEqualTo(registeredServices - (expected ? 1 : 0));
            verify(openTelemetryController, times(1)).unregisterTraceExporterService(testTraceExporterService.getName());
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
            verify(openTelemetryController, times(0)).configureMeterProvider();

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
         * A noop {@link DynamicallyActivatableService trace exporter service} for testing
         */
        class TestTraceExporterService extends DynamicallyActivatableService {

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
            verify(openTelemetryController, times(1)).configureMeterProvider();
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
         * A noop {@link DynamicallyActivatableService metric exporter service} for testing
         */
        class TestMetricsExporterService extends DynamicallyActivatableMetricsExporterService {

            @Override
            public MetricReader getNewMetricReader() {
                return PeriodicMetricReader.create(LoggingMetricExporter.create());
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
}