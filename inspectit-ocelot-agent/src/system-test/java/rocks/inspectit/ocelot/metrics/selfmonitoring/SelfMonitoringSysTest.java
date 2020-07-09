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

class SelfMonitoringSysTest extends MetricsSysTestBase {

    //acquire the impl for clearing recorded stats for test purposes
    private static final ViewManager viewManager = Stats.getViewManager();

    @Test
    void metricsRecorders() {

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {

            ViewData inspectITDuration = viewManager.getView(View.Name.create("inspectit/self/duration"));

            assertThat(inspectITDuration).isNotNull();
            Map<List<TagValue>, AggregationData> aggregationMap = inspectITDuration.getAggregationMap();

            assertThat(aggregationMap).isNotNull().isNotEmpty();
            assertThat(aggregationMap.keySet()).flatExtracting(tags -> tags)
                    .contains(TagValue.create("ProcessorMetricsRecorder"))
                    .contains(TagValue.create("ClassLoaderMetricsRecorder"))
                    .contains(TagValue.create("DiskMetricsRecorder"));

            assertThat(aggregationMap.values()).allSatisfy(data -> {
                assertThat(data).isInstanceOf(AggregationData.SumDataDouble.class);
                assertThat(((AggregationData.SumDataDouble) data).getSum()).isNotNegative().isNotZero();
            });
        });
    }

}
