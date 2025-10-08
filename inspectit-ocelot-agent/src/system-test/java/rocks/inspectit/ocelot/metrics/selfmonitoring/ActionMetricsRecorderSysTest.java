package rocks.inspectit.ocelot.metrics.selfmonitoring;

import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.metrics.MetricsSysTestBase;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests the {@link rocks.inspectit.ocelot.core.selfmonitoring.ActionsMetricsRecorder}
 */
public class ActionMetricsRecorderSysTest extends MetricsSysTestBase {

    public static double blackhole;

    public void trigger() {
        blackhole = Math.random();
    }

    @Test
    public void testActionsMetricsRecorder() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            HistogramPointData executionData = MetricTestUtils.getDataForHistogramView("inspectit_self_action_execution_time", emptyMap());

            // record some measurements (that have been specified in ActionMetricsRecorderTest.yml)
            trigger();

            assertThat(executionData).isNotNull();

            // ensure that attributes are present
            assertThat(executionData.getAttributes().asMap()).isNotEmpty();

            // ensure that the values are sane
            double executionTime = executionData.getSum();
            long count = executionData.getCount();

            assertThat(executionTime).isGreaterThan(0);
            assertThat(count).isGreaterThan(0);
        });
    }
}
