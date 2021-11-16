package rocks.inspectit.ocelot.metrics.selfmonitoring;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.metrics.MetricsSysTestBase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests the {@link rocks.inspectit.ocelot.core.selfmonitoring.ActionsMetricsRecorder}
 */
public class ActionMetricsRecorderTestAgent extends MetricsSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    @Test
    public void testActionsMetricsRecorder() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {

            // TODO fire some fake action

            ViewData actionExecutionTimes = viewManager.getView(View.Name.create("inspectit/self/actions/execution-time"));
            ViewData actionExecutionCounts = viewManager.getView(View.Name.create("inspectit/self/actions/count"));

            // fake record some measurements

            assertThat(actionExecutionTimes).isNotNull();
            assertThat(actionExecutionTimes.getAggregationMap()).isNotNull().isNotEmpty();

            Map.Entry<List<TagValue>, AggregationData> executionTime = actionExecutionTimes.getAggregationMap()
                    .entrySet()
                    .stream()
                    .findFirst()
                    .get();

            // ensure that tags are present
            assertThat(executionTime.getKey()).isNotEmpty();

            System.out.println("print keys");
            System.out.println(executionTime.getKey().toArray());

            // ensure sanity of values
            long executionTimeVal = ((AggregationData.LastValueDataLong) executionTime.getValue()).getLastValue();
            assertThat(executionTimeVal).isGreaterThan(0);

        });
    }

}
