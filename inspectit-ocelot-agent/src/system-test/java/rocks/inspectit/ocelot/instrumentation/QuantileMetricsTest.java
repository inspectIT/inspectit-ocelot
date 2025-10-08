package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class QuantileMetricsTest extends InstrumentationSysTestBase {

    /**
     * Instrumented, see the QuantileMetricsTest.yml config file
     */
    private static void record(double value, String tag) {}

    @BeforeAll
    static void waitForClassInstrumentation() {
        TestUtils.waitForClassInstrumentation(QuantileMetricsTest.class, true, 30, TimeUnit.SECONDS);
    }

    @Test
    void checkQuantileMetricsExported() {
        for (int i = 1; i < 1000; i++) {
            record(1000 + i, "bar");
        }

        TestUtils.waitForTimeWindowRecorder();

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(MetricTestUtils.getDataForView("quantiles_test_min",
                    ImmutableMap.of("foo", "bar")))
                    .isNotNull()
                    .isInstanceOfSatisfying(DoublePointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1001.0)
                    );

            assertThat(MetricTestUtils.getDataForView("quantiles_test_max",
                    ImmutableMap.of("foo", "bar")))
                    .isNotNull()
                    .isInstanceOfSatisfying(DoublePointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1999.0)
                    );

            assertThat(MetricTestUtils.getDataForView("quantiles_test",
                    ImmutableMap.of("foo", "bar", "quantile", "0.5")))
                    .isNotNull()
                    .isInstanceOfSatisfying(DoublePointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1500.0)
                    );

            assertThat(MetricTestUtils.getDataForView("quantiles_test",
                    ImmutableMap.of("foo", "bar", "quantile", "0.9")))
                    .isNotNull()
                    .isInstanceOfSatisfying(DoublePointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1900.0)
                    );

            assertThat(MetricTestUtils.getDataForView("quantiles_test",
                    ImmutableMap.of("foo", "bar", "quantile", "0.95")))
                    .isNotNull()
                    .isInstanceOfSatisfying(DoublePointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1950.0)
                    );

            assertThat(MetricTestUtils.getDataForView("quantiles_test",
                    ImmutableMap.of("foo", "bar", "quantile", "0.99")))
                    .isNotNull()
                    .isInstanceOfSatisfying(DoublePointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1990.0)
                    );
        });
    }
}
