package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingTraceExporterServiceIntTest extends SpringTestBase {

    public static final String INSTRUMENTATION_NAME = "rocks.inspectit.ocelot.instrumentation";

    public static final String INSTRUMENTATION_VERSION = "0.0.1";

    @RegisterExtension
    LogCapturer spanLogs = LogCapturer.create().captureForType(LoggingSpanExporter.class);

    @Autowired
    LoggingTraceExporterService service;

    @Autowired
    InspectitEnvironment environment;

    @BeforeEach
    void enableService() {
        localSwitch(true);
    }

    private void localSwitch(boolean enabled) {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.logging.enabled", enabled);
        });
    }

    @Nested
    class EnableDisable {

        @Test
        void testMasterSwitch() {
            updateProperties(props -> {
                props.setProperty("inspectit.tracing.enabled", "false");
            });
            assertThat(service.isEnabled()).isFalse();
        }

        @Test
        void testLocalSwitch() {
            localSwitch(false);
            assertThat(service.isEnabled()).isFalse();
        }
    }

    @Nested
    class OpenTelemetryLogging {

        private Tracer getTracer() {
            return GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
        }

        private void makeSpans() {
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
        }

        @Test
        void verifyOpenTelemetryTraceSent() throws InterruptedException {
            assertThat(service.isEnabled()).isTrue();

            makeSpans();

            Awaitility.waitAtMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
                // assert that two traces have been logged
                assertThat(spanLogs.getEvents()).hasSize(2);
                // and the last contains our 'childOne'
                assertThat(spanLogs.getEvents().get(0).getMessage()).contains("openTelemetryChildSpan");
            });

            // turn off trace exporter
            localSwitch(false);

            // wait until everything is flushed
            Thread.sleep(500);

            // get number of logged events
            int numEvents = spanLogs.size();

            // make sure no more spans are recorded
            Thread.sleep(5000);
            assertThat(spanLogs.size()).isEqualTo(numEvents);

            // turn the trace exporter on again
            localSwitch(true);
            Thread.sleep(1000);
            makeSpans();
            // wait until the new spans are exported to the log
            Awaitility.waitAtMost(5, TimeUnit.SECONDS)
                    .pollInterval(2, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(spanLogs.size()).isEqualTo(numEvents + 2));

        }

        @Test
        void testLoggingExporterDisabled() throws InterruptedException {
            assertThat(service.isEnabled()).isTrue();

            // disable exporter service
            localSwitch(false);

            assertThat(service.isEnabled()).isFalse();

            makeSpans();

            // make sure that no spans were exported
            assertThat(spanLogs.getEvents()).hasSize(0);
        }

    }

    @Nested
    class OpenCensusLogging {

        @Test
        void testTracerRestart() {
            Tracer tracer = OpenCensusShimUtils.getOpenTelemetryTracerOfOpenTelemetrySpanBuilderImpl();
            localSwitch(false);
            Awaitility.waitAtMost(5, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(environment.getCurrentConfig()
                            .getExporters()
                            .getTracing()
                            .getLogging()
                            .isEnabled()).isFalse());
            localSwitch(true);
            OpenCensusShimUtils.updateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl();
            Awaitility.waitAtMost(5, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(environment.getCurrentConfig()
                            .getExporters()
                            .getTracing()
                            .getLogging()
                            .isEnabled()).isTrue());
            Tracer newTracer = OpenCensusShimUtils.getOpenTelemetryTracerOfOpenTelemetrySpanBuilderImpl();

            assertThat(tracer).isNotSameAs(newTracer);

        }

        @Test
        void verifyOpenCensusTraceSent() throws InterruptedException {
            assertThat(service.isEnabled()).isTrue();

            // make some spans
            makeSpans();

            // assert that both spans are logged
            assertThat(spanLogs.getEvents()).hasSize(2);
            assertThat(spanLogs.getEvents().get(0).getMessage()).contains("openCensusChild");

            int numEvents = spanLogs.size();

            // turn off tracing exporter
            localSwitch(false);
            // make sure no more spans are recorded
            Awaitility.await().untilAsserted(() -> assertThat(service.isEnabled()).isFalse());
            assertThat(spanLogs.size()).isEqualTo(numEvents);

            // turn tracing exporter back on
            localSwitch(true);
            Awaitility.await().untilAsserted(() -> assertThat(service.isEnabled()).isTrue());

            // make spans
            makeSpans();
            // verify logging of spans
            Awaitility.waitAtMost(10, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(spanLogs.size()).isEqualTo(numEvents + 2));
        }

        private io.opencensus.trace.Tracer getTracer() {
            return Tracing.getTracer();
        }

        private void makeSpans() {
            // get OC tracer and start spans
            io.opencensus.trace.Tracer tracer = getTracer();

            // start span and nested span
            try (io.opencensus.common.Scope scope = tracer.spanBuilder("openCensusParent").startScopedSpan()) {
                try (io.opencensus.common.Scope childScope = tracer.spanBuilder("openCensusChild").startScopedSpan()) {
                    io.opencensus.trace.Span span = tracer.getCurrentSpan();
                    span.addAnnotation("invoking stuff in openCensusChild");
                }
            }
        }

    }
}
