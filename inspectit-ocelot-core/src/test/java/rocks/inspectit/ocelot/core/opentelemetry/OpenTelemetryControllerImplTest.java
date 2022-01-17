package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableMetricsExporterService;
import rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableTraceExporterService;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link OpenTelemetryControllerImpl}
 */
@ExtendWith(MockitoExtension.class)
class OpenTelemetryControllerImplTest {

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

            System.out.println(testTraceExporterService.getName() + " - " + openTelemetryController.registeredTraceExportServices.size());
            // register service
            registerTestTraceExporterServiceAndVerify(true);
            System.out.println(openTelemetryController.registeredTraceExportServices.size());
            // unregistering should now succeed
            unregisterTestTraceExporterServiceAndVerify(true);
        }

        @Test
        void testConfigureTracing() {
            TracerProvider tracerProvider = GlobalOpenTelemetry.getTracerProvider();
            // register test service
            registerTestTraceExporterServiceAndVerify(true);
            // configure OTEL
            assertThat(openTelemetryController.configureOpenTelemetry()).isTrue();
            // tracing should have changed but metrics not
            verify(openTelemetryController, times(1)).configureTracing(any(InspectitConfig.class));
            verify(openTelemetryController, times(0)).configureMeterProvider(any(InspectitConfig.class));

            // verify that the tracer provider does not change after tracing has been (re-)configured
            assertThat(tracerProvider).isEqualTo(tracerProvider);
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
            MeterProviderImpl meterProviderImpl = openTelemetryController.getMeterProvider();

            MeterProvider globalMeterProvider = GlobalMeterProvider.get();
            // register some service
            registerTestMetricExporterServiceAndVerify(true);
            // configure OTEL
            assertThat(openTelemetryController.configureOpenTelemetry()).isTrue();

            // verify that meter provider was configured but not tracing
            verify(openTelemetryController, times(1)).configureMeterProvider(any(InspectitConfig.class));
            verify(openTelemetryController, times(0)).configureTracing(any(InspectitConfig.class));

            // (global) meter provider should not have changed during configuration
            assertThat(openTelemetryController.getMeterProvider()).isSameAs(meterProviderImpl);
            assertThat(GlobalMeterProvider.get()).isSameAs(globalMeterProvider);

        }

        @Test
        void testShutdown() {
            openTelemetryController.shutdown();
            assertThat(null == GlobalMeterProvider.get());
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

}