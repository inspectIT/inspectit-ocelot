package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opencensus.stats.*;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link LoggingExporterService}
 */
public class LoggingExporterServiceTest extends SpringTestBase {

    public static final String INSTRUMENTATION_NAME = "rocks.inspectit.ocelot.instrumentation";

    public static final String INSTRUMENTATION_VERSION = "0.0.1";

    @RegisterExtension
    LogCapturer spanLogs = LogCapturer.create().captureForType(LoggingSpanExporter.class);

    @RegisterExtension
    LogCapturer metricLogs = LogCapturer.create().captureForType(LoggingMetricExporter.class);

    @SpyBean
    @Autowired
    LoggingExporterService service;

    @Autowired
    InspectitEnvironment environment;

    @BeforeEach
    private void enableService() {
        localSwitch(true);
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.logging.export-interval", environment.getCurrentConfig()
                    .getExporters()
                    .getMetrics()
                    .getLogging()
                    .getExportInterval());
        });
    }

    private void localSwitch(boolean enabled) {
        localSwitchMetrics(enabled);
        localSwitchTracing(enabled);
    }

    private void localSwitchMetrics(boolean enabled) {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.logging.enabled", enabled);
        });
    }

    private void localSwitchTracing(boolean enabled) {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.logging.enabled", enabled);
        });
    }

    @Nested
    class EnableDisable {

        @Test
        void testMasterSwitch() throws Exception {
            updateProperties(props -> {
                props.setProperty("inspectit.metrics.enabled", "false");
            });
            // TODO: test service disabled
            assertThat(service.isEnabled()).isFalse();
        }

        @Test
        void testLocalSwitch() throws Exception {
            localSwitch(false);
            assertThat(service.isEnabled()).isFalse();
        }

        @Test
        void testLoggingTraceExporterSwitch() {
            assertThat(service.isEnabled()).isTrue();
            localSwitchTracing(false);
            assertThat(service.isEnabled()).isTrue();
            assertThat(environment.getCurrentConfig().getExporters().getTracing().getLogging().isEnabled()).isFalse();
            localSwitchTracing(true);
            assertThat(environment.getCurrentConfig().getExporters().getTracing().getLogging().isEnabled()).isTrue();
        }

        @Test
        void testLoggingMetricExporterSwitch() {
            assertThat(service.isEnabled()).isTrue();
            localSwitchMetrics(false);
            assertThat(service.isEnabled()).isTrue();
            assertThat(environment.getCurrentConfig().getExporters().getMetrics().getLogging().isEnabled()).isFalse();
            localSwitchMetrics(true);
            assertThat(environment.getCurrentConfig().getExporters().getMetrics().getLogging().isEnabled()).isTrue();
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
        void testOpenTelemetryTraceLogging() throws InterruptedException {
            assertThat(service.isEnabled()).isTrue();
            localSwitchMetrics(false);
            assertThat(environment.getCurrentConfig().getExporters().getTracing().getLogging().isEnabled());

            makeSpans();

            Awaitility.waitAtMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
                // assert that two traces have been logged
                assertThat(spanLogs.getEvents()).hasSize(2);
                // and the last contains our 'childOne'
                assertThat(spanLogs.getEvents().get(0).getMessage()).contains("openTelemetryChildSpan");
            });

            // turn off trace exporter
            localSwitchTracing(false);

            // wait until everything is flushed
            Thread.sleep(500);

            // get number of logged events
            int numEvents = spanLogs.size();

            // make sure no more spans are recorded
            Thread.sleep(5000);
            assertThat(spanLogs.size()).isEqualTo(numEvents);

            // turn the trace exporter on again
            localSwitchTracing(true);
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
            io.opencensus.trace.Tracer tracer = getTracer();
            localSwitchTracing(false);
            Awaitility.waitAtMost(5, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(environment.getCurrentConfig()
                            .getExporters()
                            .getTracing()
                            .getLogging()
                            .isEnabled()).isFalse());
            localSwitchTracing(true);
            OpenCensusShimUtils.updateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl();
            Awaitility.waitAtMost(5, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(environment.getCurrentConfig()
                            .getExporters()
                            .getTracing()
                            .getLogging()
                            .isEnabled()).isTrue());
            io.opencensus.trace.Tracer newTracer = getTracer();

            assertThat(tracer).isNotSameAs(newTracer);

        }

        @Test
        void testOpenCensusTraceLogging() throws InterruptedException {
            assertThat(service.isEnabled()).isTrue();
            // turn off metrics to avoid spamming
            localSwitchMetrics(false);

            System.out.println(getTracer().toString() + " - " + getTracer().hashCode());
            // make some spans
            makeSpans();

            // assert that both spans are logged
            assertThat(spanLogs.getEvents()).hasSize(2);
            assertThat(spanLogs.getEvents().get(0).getMessage()).contains("openCensusChild");

            int numEvents = spanLogs.size();

            // turn off tracing exporter
            localSwitchTracing(false);
            // make sure no more spans are recorded
            Thread.sleep(500);
            assertThat(spanLogs.size()).isEqualTo(numEvents);

            // turn tracing exporter back on
            localSwitchTracing(true);
            Thread.sleep(500);

            System.out.println(getTracer().toString() + " - " + getTracer().hashCode());
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

        StatsRecorder statsRecorder = Stats.getStatsRecorder();

        @Test
        void testOpenCensusMetricLogging() throws InterruptedException {
            // change export interval
            updateProperties(props -> {
                props.setProperty("inspectit.exporters.metrics.logging.export-interval", "500ms");
            });
            assertThat(service.isEnabled()).isTrue();
            // shut the trace exporter off
            localSwitchTracing(false);
            assertThat(environment.getCurrentConfig().getExporters().getMetrics().getLogging().isEnabled()).isTrue();

            // capture some metrics
            captureOpenCensusMetrics();

            // wait until the metrics are exported
            Awaitility.waitAtMost(15, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(metricLogs.getEvents().size()).isGreaterThan(0);
                // assert that the latest metric is our custom log
                assertThat(metricLogs.getEvents()
                        .get(metricLogs.getEvents().size() - 1)
                        .getArgumentArray()[0].toString()).contains("oc.desc");

            });

            // now turn the exporter off and make sure that no more metrics are exported to the log
            localSwitchMetrics(false);
            // wait until everything is flushed
            Thread.sleep(500);
            int numEvents = metricLogs.getEvents().size();

            Thread.sleep(environment.getCurrentConfig()
                    .getExporters()
                    .getMetrics()
                    .getLogging()
                    .getExportInterval()
                    .toMillis() + 1000);
            assertThat(metricLogs.getEvents().size()).isEqualTo(numEvents);

        }

        private void captureOpenCensusMetrics() {
            Measure.MeasureLong measure = Measure.MeasureLong.create("oc.measure", "oc.desc", "oc.unit");
            Stats.getViewManager()
                    .registerView(View.create(View.Name.create("oc.sum"), "oc.desc", measure, Aggregation.Count.create(), Collections.emptyList()));

            statsRecorder.newMeasureMap().put(measure, 1).record();

        }
    }

}
