package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.TracerProvider;
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
import rocks.inspectit.ocelot.core.exporter.DynamicMultiSpanExporter;
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
        openTelemetryController.env = env;
        openTelemetryController.init();
        openTelemetryController.start();
    }

    @Nested
    class ConfigureTracing {

        @Spy
        DynamicMultiSpanExporter spanExporter;

        @Spy
        TestTraceService testTraceService = new TestTraceService();

        @BeforeEach
        protected void init() {
            openTelemetryController.setSpanExporter(spanExporter);
            clearInvocations(openTelemetryController);
        }

        /**
         * Registers the {@link #testTraceService} to the {@link #openTelemetryController} and verifies the interactions
         *
         * @param expected Whether the registration is expected to succeed.
         */
        protected void registerTestTraceExporterServiceAndVerify(boolean expected) {
            int registeredServices = openTelemetryController.registeredTraceExportServices.size();
            assertThat(openTelemetryController.registerTraceExporterService(testTraceService)).isEqualTo(expected);
            assertThat(openTelemetryController.registeredTraceExportServices.size()).isEqualTo(registeredServices + 1);
            verify(openTelemetryController, times(1)).registerTraceExporterService(testTraceService);
            verify(openTelemetryController, times(expected ? 1 : 0)).notifyTracingSettingsChanged();
            verify(spanExporter, times(1)).registerSpanExporter(testTraceService.getName(), testTraceService.getSpanExporter());
            verifyNoMoreInteractions(openTelemetryController, spanExporter);
            clearInvocations(openTelemetryController, spanExporter);
        }

        /**
         * Unregisters the {@link #testTraceService} from the {@link #openTelemetryController} and verifies the interactions
         *
         * @param expected Whether the unregistration is expected to succeed.
         */
        protected void unregisterTestTraceExporterServiceAndVerify(boolean expected) {
            int registeredServices = openTelemetryController.registeredTraceExportServices.size();
            assertThat(openTelemetryController.unregisterTraceExporterService(testTraceService)).isEqualTo(expected);
            assertThat(openTelemetryController.registeredTraceExportServices.size()).isEqualTo(registeredServices - 1);
            verify(openTelemetryController, times(1)).unregisterTraceExporterService(testTraceService);
            verify(openTelemetryController, times(expected ? 1 : 0)).notifyTracingSettingsChanged();
            verify(spanExporter, times(1)).unregisterSpanExporter(testTraceService.getName());
            verifyNoMoreInteractions(openTelemetryController, spanExporter);
            clearInvocations(openTelemetryController, spanExporter);
        }

        @DirtiesContext
        @Test
        void testTraceExporterServiceRegistration() {

            // check that service is successfully registered
            registerTestTraceExporterServiceAndVerify(true);

            // service cannot be registered twice
            registerTestTraceExporterServiceAndVerify(false);
        }

        @Test
        void testTraceExporterServiceUnregistration() {
            // when a service is not registered, unregistration fails
            unregisterTestTraceExporterServiceAndVerify(false);

            // register service
            registerTestTraceExporterServiceAndVerify(true);

            // unregistering should now succeed
            unregisterTestTraceExporterServiceAndVerify(false);
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
            assertThat(openTelemetryController.isStopped());
            assertThat(OpenTelemetry.noop() == GlobalOpenTelemetry.get());
        }

    }

    /**
     * A noop {@link DynamicallyActivatableTraceExporterService} for testing
     */
    static class TestTraceService extends DynamicallyActivatableTraceExporterService {

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