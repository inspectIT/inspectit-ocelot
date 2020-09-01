package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.export.TimeSeries;
import io.opencensus.metrics.export.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class QuantileMetricsTest extends InstrumentationSysTestBase {

    /**
     * Instrumented, see the QuantileMetricsTest.yml config file
     *
     * @param value
     */
    private static void record(double value, String tag) {
    }

    @BeforeAll
    static void waitForInstrumentation() {
        TestUtils.waitForClassInstrumentation(QuantileMetricsTest.class, true, 30, TimeUnit.SECONDS);
    }

    @Test
    void checkQuantileMetricsExported() {
        for (int i = 1; i < 1000; i++) {
            record(1000 + i, "bar");
        }
        TestUtils.waitForOpenCensusQueueToBeProcessed();
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            TimeSeries median = TestUtils.getTimeseries("quantiles/test/view", ImmutableMap.of("foo", "bar", "quantile", "0.5"));
            TimeSeries p90 = TestUtils.getTimeseries("quantiles/test/view", ImmutableMap.of("foo", "bar", "quantile", "0.9"));
            TimeSeries p95 = TestUtils.getTimeseries("quantiles/test/view", ImmutableMap.of("foo", "bar", "quantile", "0.95"));
            TimeSeries p99 = TestUtils.getTimeseries("quantiles/test/view", ImmutableMap.of("foo", "bar", "quantile", "0.99"));
            TimeSeries min = TestUtils.getTimeseries("quantiles/test/view_min", ImmutableMap.of("foo", "bar"));
            TimeSeries max = TestUtils.getTimeseries("quantiles/test/view_max", ImmutableMap.of("foo", "bar"));

            assertThat(median.getPoints()).hasSize(1);
            assertThat(median.getPoints().get(0).getValue()).isEqualTo(Value.doubleValue(1500.0));

            assertThat(p90.getPoints()).hasSize(1);
            assertThat(p90.getPoints().get(0).getValue()).isEqualTo(Value.doubleValue(1900.0));

            assertThat(p95.getPoints()).hasSize(1);
            assertThat(p95.getPoints().get(0).getValue()).isEqualTo(Value.doubleValue(1950.0));

            assertThat(p99.getPoints()).hasSize(1);
            assertThat(p99.getPoints().get(0).getValue()).isEqualTo(Value.doubleValue(1990.0));

            assertThat(min.getPoints()).hasSize(1);
            assertThat(min.getPoints().get(0).getValue()).isEqualTo(Value.doubleValue(1001.0));

            assertThat(max.getPoints()).hasSize(1);
            assertThat(max.getPoints().get(0).getValue()).isEqualTo(Value.doubleValue(1999.0));
        });
    }
}
