package rocks.inspectit.ocelot.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ProcessorMetricsSysTest extends MetricsSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    @Test
    public void testProcessorCountCapturing() {
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData procCountData = viewManager.getView(View.Name.create("system/cpu/count"));
            assertThat(procCountData.getAggregationMap()).isNotEmpty();

            Map.Entry<List<TagValue>, AggregationData> procCount = procCountData.getAggregationMap().entrySet().stream().findFirst().get();

            //ensure that tags are present
            assertThat(procCount.getKey()).isNotEmpty();

            //ensure that the values are sane
            long processorCount = ((AggregationData.LastValueDataLong) procCount.getValue()).getLastValue();

            assertThat(processorCount).isEqualTo(Runtime.getRuntime().availableProcessors());
        });
    }
}
