package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opencensus.stats.*;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundLongCounter;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingMetricsExporterServiceTest extends SpringTestBase {

    @RegisterExtension
    LogCapturer metricLogs = LogCapturer.create().captureForType(LoggingMetricExporter.class);

    @Autowired
    LoggingMetricExporterService service;

    @Autowired
    InspectitEnvironment environment;

    @BeforeEach
    void enableService() {
        localSwitch(true);
    }

    @AfterEach
    void disableService() {
        localSwitch(false);
    }

    private void localSwitch(boolean enabled) {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.logging.enabled", enabled);
        });
    }

    @Nested
    class EnableDisable {

        @Test
        void testMasterSwitch() {
            updateProperties(props -> {
                props.setProperty("inspectit.metrics.enabled", "false");
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

        @Test
        void verifyOpenTelemetryMetricsWritten() {
            // change export interval
            updateProperties(props -> {
                props.setProperty("inspectit.exporters.metrics.logging.export-interval", "500ms");
            });

            assertThat(service.isEnabled()).isTrue();

            // get the meter and create a counter
            Meter meter = GlobalMeterProvider.get()
                    .meterBuilder("rocks.inspectit.ocelot")
                    .setInstrumentationVersion("0.0.1")
                    .build();
            LongCounter counter = meter.counterBuilder("processed_jobs")
                    .setDescription("Processed jobs")
                    .setUnit("1")
                    .build();
            BoundLongCounter workCounter = counter.bind(Attributes.of(AttributeKey.stringKey("Key"), "SomeWork"));

            // record counter
            workCounter.add(1);

            // verify that the metric has been exported to the log
            Awaitility.waitAtMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(metricLogs.getEvents().size()).isGreaterThan(0);
                assertThat(metricLogs.getEvents()).anyMatch(evt -> evt.getArgumentArray() != null && evt.getArgumentArray()[0].toString()
                        .contains("processed_jobs"));
            });
        }
    }

    @Nested
    class OpenCensusLogging {

        StatsRecorder statsRecorder = Stats.getStatsRecorder();

        @Test
        void verifyOpenCensusMetricsWritten() throws InterruptedException {
            // change export interval
            updateProperties(props -> {
                props.setProperty("inspectit.exporters.metrics.logging.export-interval", "500ms");
            });
            assertThat(service.isEnabled()).isTrue();

            // capture some metrics
            captureOpenCensusMetrics();

            // wait until the metrics are exported
            Awaitility.waitAtMost(15, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(metricLogs.getEvents().size()).isGreaterThan(0);
                // assert that the latest metric is our custom log
                assertThat(metricLogs.getEvents()).anyMatch(evt -> evt.getArgumentArray() != null && evt.getArgumentArray()[0].toString()
                        .contains("oc.desc"));

            });

            // now turn the exporter off and make sure that no more metrics are exported to the log
            localSwitch(false);
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
