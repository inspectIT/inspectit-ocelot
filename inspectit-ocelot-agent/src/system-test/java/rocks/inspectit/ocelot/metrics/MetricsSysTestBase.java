package rocks.inspectit.ocelot.metrics;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.BeforeEach;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class MetricsSysTestBase {

    @BeforeEach
    void flushMetrics() {
        TestUtils.waitForAgentInitialization();
        MetricTestUtils.initializeMetricReader();
    }

    /**
     * Checks, if the metric was recorded with the expected value and some attributes
     */
    protected void assertMetric(String metricName, int expectedValue) {
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricTestUtils.getDataForView(metricName, emptyMap()))
                        .isNotNull()
                        .isInstanceOfSatisfying(LongPointData.class, (pointData) -> {
                            assertThat(pointData.getValue()).isEqualTo(expectedValue);
                            assertThat(pointData.getAttributes().asMap()).isNotEmpty();
                        })
        );
    }

    /**
     * Checks, if the metric was recorded at all with some attributes
     */
    protected void assertMetric(String metricName) {
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricTestUtils.getDataForView(metricName, emptyMap()))
                        .isNotNull()
                        .satisfies((pointData) ->
                                assertThat(pointData.getAttributes().asMap()).isNotEmpty())
        );
    }
}
