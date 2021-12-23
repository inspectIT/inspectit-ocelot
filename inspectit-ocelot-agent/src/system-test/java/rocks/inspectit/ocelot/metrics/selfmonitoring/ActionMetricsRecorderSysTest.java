package rocks.inspectit.ocelot.metrics.selfmonitoring;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.metrics.MetricsSysTestBase;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests the {@link rocks.inspectit.ocelot.core.selfmonitoring.ActionsMetricsRecorder}
 */
public class ActionMetricsRecorderSysTest extends MetricsSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    public void trigger() {
        System.out.println("trigger method");
    }

    @Test
    public void testActionsMetricsRecorder() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {

            // get the views
            ViewData actionExecutionTimes = viewManager.getView(View.Name.create("inspectit/self/action/execution-time"));
            ViewData actionExecutionCounts = viewManager.getView(View.Name.create("inspectit/self/action/count"));

            // record some measurements (that have been specified in ActionMetricsRecorderTest.yml)
            trigger();

            // verify that execution-time and count has been recorded
            assertThat(actionExecutionTimes).isNotNull();
            assertThat(actionExecutionTimes.getAggregationMap()).isNotNull().isNotEmpty();
            assertThat(actionExecutionCounts).isNotNull();
            assertThat(actionExecutionCounts.getAggregationMap()).isNotNull().isNotEmpty();

            Map.Entry<List<TagValue>, AggregationData> executionTime = actionExecutionTimes.getAggregationMap()
                    .entrySet()
                    .stream()
                    .findFirst()
                    .get();

            Map.Entry<List<TagValue>, AggregationData> executionCount = actionExecutionCounts.getAggregationMap()
                    .entrySet()
                    .stream()
                    .findFirst()
                    .get();

            // ensure that tags are present
            assertThat(executionTime.getKey()).isNotEmpty();
            assertThat(executionCount.getKey()).isNotEmpty();

            System.out.println("print keys");
            System.out.println(Arrays.toString(executionTime.getKey().toArray()));

            // ensure sanity of values
            long executionTimeVal = ((AggregationData.SumDataLong) executionTime.getValue()).getSum();
            assertThat(executionTimeVal).isGreaterThan(0);
            long executionCountVal = ((AggregationData.CountData) executionCount.getValue()).getCount();
            assertThat(executionCountVal).isGreaterThan(0);

        });
    }

}
