package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link LoggingTraceExporterService}
 */
@TestPropertySource(properties = "inspectit.tracing.max-export-batch-size:2")
@DirtiesContext
public class LoggingTraceExporterServiceIntTest extends SpringTestBase {

    public static final String INSTRUMENTATION_NAME = "rocks.inspectit.ocelot.instrumentation";

    public static final String INSTRUMENTATION_VERSION = "0.0.1";

    @RegisterExtension
    LogCapturer spanLogs = LogCapturer.create().captureForType(LoggingSpanExporter.class);

    @Autowired
    LoggingTraceExporterService service;

    @BeforeAll
    static void beforeAll() {
        // enable jul -> slf4j bridge
        // this is necessary as OTEL logs to jul, but we use the LogCapturer with logback
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
    }

    @AfterAll
    static void afterAll() {
        if (SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.uninstall();
        }
    }

    @BeforeEach
    void enableService() {
        localSwitch(true);
        masterSwitch(true);
    }

    private void localSwitch(boolean enabled) {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.logging.enabled", enabled);
        });
    }

    private void masterSwitch(boolean enabled) {
        updateProperties(props -> {
            props.setProperty("inspectit.tracing.enabled", enabled);
        });
    }

    @Nested
    class EnableDisable {

        @DirtiesContext
        @Test
        void testMasterSwitch() {
            masterSwitch(false);
            assertThat(service.isEnabled()).isFalse();
        }

        @DirtiesContext
        @Test
        void testLocalSwitch() {
            assertThat(service.isEnabled()).isTrue();
            localSwitch(false);
            assertThat(service.isEnabled()).isFalse();
        }
    }

    @Nested
    class OpenTelemetryLogging {

        private Tracer getTracer() {
            return GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
        }

        private void makeSpansAndFlush() {
            // start span and nested span
            Span parentSpan = getTracer().spanBuilder("openTelemetryParentSpan").startSpan();
            try (Scope scope = parentSpan.makeCurrent()) {
                Span childSpan = getTracer().spanBuilder("openTelemetryChildSpan").startSpan();
                try (Scope child = childSpan.makeCurrent()) {
                    // do sth
                } finally {
                    childSpan.end();
                }
            } finally {
                parentSpan.end();
            }

            // flush pending spans
            Instances.openTelemetryController.flush();
        }

        @DirtiesContext
        @Test
        void verifyOpenTelemetryTraceSent() throws InterruptedException {
            assertThat(service.isEnabled()).isTrue();

            makeSpansAndFlush();

            Awaitility.waitAtMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
                // assert that two traces have been logged
                assertThat(spanLogs.getEvents()).hasSize(2);
                // and the last contains our 'childOne'
                assertThat(spanLogs.getEvents().get(0).getMessage()).contains("openTelemetryChildSpan");
            });

            // get number of logged events
            int numEvents = spanLogs.size();

            // turn off trace exporter
            localSwitch(false);

            // wait until the service is shut down
            Awaitility.waitAtMost(5, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(service.isEnabled()).isFalse());

            // make sure no more spans are recorded
            Thread.sleep(5000); // TODO: is there a better way than to sleep?
            assertThat(spanLogs.size()).isEqualTo(numEvents);

            // turn the trace exporter on again
            localSwitch(true);
            Awaitility.waitAtMost(1, TimeUnit.SECONDS).untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

            makeSpansAndFlush();

            // wait until the new spans are exported to the log
            Awaitility.waitAtMost(10, TimeUnit.SECONDS)
                    .pollInterval(2, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(spanLogs.size()).isEqualTo(numEvents + 2));

        }

        @DirtiesContext
        @Test
        void testLoggingExporterDisabled() throws InterruptedException {
            assertThat(service.isEnabled()).isTrue();

            // disable exporter service
            localSwitch(false);

            assertThat(service.isEnabled()).isFalse();

            makeSpansAndFlush();

            // make sure that no spans were exported
            assertThat(spanLogs.getEvents()).hasSize(0);
        }

    }

    @Nested
    class OpenCensusLogging {

        @DirtiesContext
        @Test
        void verifyOpenCensusTraceSent() throws InterruptedException {
            assertThat(service.isEnabled()).isTrue();

            // make some spans
            makeSpansAndFlush();

            // assert that both spans are logged
            Awaitility.waitAtMost(5, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(spanLogs.getEvents()).hasSize(2));
            assertThat(spanLogs.getEvents().get(0).getMessage()).contains("openCensusChild");

            int numEvents = spanLogs.size();

            // turn off tracing exporter
            localSwitch(false);
            // make sure no more spans are recorded
            Awaitility.waitAtMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(service.isEnabled()).isFalse());
            assertThat(spanLogs.size()).isEqualTo(numEvents);

            // turn tracing exporter back on
            localSwitch(true);
            Awaitility.waitAtMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

            // make spans
            makeSpansAndFlush();
            // verify logging of spans
            Awaitility.waitAtMost(10, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(spanLogs.size()).isEqualTo(numEvents + 2));
        }

        private io.opencensus.trace.Tracer getTracer() {
            return Tracing.getTracer();
        }

        private void makeSpansAndFlush() {
            // get OC tracer and start spans
            io.opencensus.trace.Tracer tracer = getTracer();

            // start span and nested span
            try (io.opencensus.common.Scope scope = tracer.spanBuilder("openCensusParent").startScopedSpan()) {
                try (io.opencensus.common.Scope childScope = tracer.spanBuilder("openCensusChild").startScopedSpan()) {
                    io.opencensus.trace.Span span = tracer.getCurrentSpan();
                    span.addAnnotation("invoking stuff in openCensusChild");
                }
            }

            // flush pending spans
            Instances.openTelemetryController.flush();
        }

    }
}
