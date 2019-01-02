package rocks.inspectit.oce.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreadMetricsSysTest extends MetricsSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    @Test
    public void testThreadMetricsCapturing() throws Exception {
        ViewData liveData = viewManager.getView(View.Name.create("jvm/threads/live"));
        ViewData peakData = viewManager.getView(View.Name.create("jvm/threads/peak"));
        ViewData daemonData = viewManager.getView(View.Name.create("jvm/threads/daemon"));
        ViewData stateData = viewManager.getView(View.Name.create("jvm/threads/states"));

        assertThat(liveData.getAggregationMap()).isNotEmpty();
        assertThat(peakData.getAggregationMap()).isNotEmpty();
        assertThat(daemonData.getAggregationMap()).isNotEmpty();
        assertThat(stateData.getAggregationMap()).isNotEmpty();

        Map.Entry<List<TagValue>, AggregationData> liveCount = liveData.getAggregationMap().entrySet().stream().findFirst().get();
        long live = ((AggregationData.LastValueDataLong) liveCount.getValue()).getLastValue();
        Map.Entry<List<TagValue>, AggregationData> peakCount = peakData.getAggregationMap().entrySet().stream().findFirst().get();
        long peak = ((AggregationData.LastValueDataLong) peakCount.getValue()).getLastValue();
        Map.Entry<List<TagValue>, AggregationData> daemonCount = daemonData.getAggregationMap().entrySet().stream().findFirst().get();
        long daemon = ((AggregationData.LastValueDataLong) daemonCount.getValue()).getLastValue();

        long statesCount =
                liveData.getAggregationMap().entrySet().stream().map(Map.Entry::getValue)
                        .map(d -> (AggregationData.LastValueDataLong) d)
                        .mapToLong(d -> d.getLastValue())
                        .sum();

        assertThat(live).isGreaterThan(0);
        assertThat(live).isEqualTo(statesCount);
        assertThat(peak).isGreaterThanOrEqualTo(live);
        assertThat(daemon).isLessThan(live);
    }
}
