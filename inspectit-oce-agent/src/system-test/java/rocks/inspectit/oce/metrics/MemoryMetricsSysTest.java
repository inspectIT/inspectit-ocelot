package rocks.inspectit.oce.metrics;

import io.opencensus.stats.Stats;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewData;
import io.opencensus.stats.ViewManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class MemoryMetricsSysTest extends MetricsSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    /**
     * This test assumes that the JVM was started with a non-concurrent GC
     */
    @Test
    public void testMemoryCapturing() {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData usedData = viewManager.getView(View.Name.create("jvm/memory/used"));
            ViewData committedData = viewManager.getView(View.Name.create("jvm/memory/committed"));
            ViewData maxData = viewManager.getView(View.Name.create("jvm/memory/max"));
            ViewData bufCountData = viewManager.getView(View.Name.create("jvm/buffer/count"));
            ViewData bufUsedData = viewManager.getView(View.Name.create("jvm/buffer/memory/used"));
            ViewData bufCapData = viewManager.getView(View.Name.create("jvm/buffer/total/capacity"));

            assertThat(usedData.getAggregationMap()).isNotEmpty();
            assertThat(committedData.getAggregationMap()).isNotEmpty();
            assertThat(maxData.getAggregationMap()).isNotEmpty();
            assertThat(bufCountData.getAggregationMap()).isNotEmpty();
            assertThat(bufUsedData.getAggregationMap()).isNotEmpty();
            assertThat(bufCapData.getAggregationMap()).isNotEmpty();
        });
    }
}
