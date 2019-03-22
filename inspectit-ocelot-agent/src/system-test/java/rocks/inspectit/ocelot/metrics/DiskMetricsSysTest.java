package rocks.inspectit.ocelot.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class DiskMetricsSysTest extends MetricsSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    @Test
    public void testDiskMetricCapturing() {

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData freeData = viewManager.getView(View.Name.create("disk/free"));
            ViewData totalData = viewManager.getView(View.Name.create("disk/total"));

            assertThat(freeData).isNotNull();
            assertThat(totalData).isNotNull();
            assertThat(freeData.getAggregationMap()).isNotEmpty();
            assertThat(totalData.getAggregationMap()).isNotEmpty();

            Map.Entry<List<TagValue>, AggregationData> free = freeData.getAggregationMap().entrySet().stream().findFirst().get();
            Map.Entry<List<TagValue>, AggregationData> total = totalData.getAggregationMap().entrySet().stream().findFirst().get();

            //ensure that tags are present
            assertThat(free.getKey()).isNotEmpty();
            assertThat(total.getKey()).isNotEmpty();

            //ensure that the values are sane
            long freeVal = ((AggregationData.LastValueDataLong) free.getValue()).getLastValue();
            long totalVal = ((AggregationData.LastValueDataLong) total.getValue()).getLastValue();
            assertThat(freeVal).isGreaterThanOrEqualTo(0);
            assertThat(freeVal).isLessThan(totalVal);
        });
    }
}
