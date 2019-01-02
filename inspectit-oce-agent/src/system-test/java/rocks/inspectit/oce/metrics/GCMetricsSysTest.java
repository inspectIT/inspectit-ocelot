package rocks.inspectit.oce.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GCMetricsSysTest extends MetricsSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    /**
     * This test assumes that the JVM was started with a non-concurrent GC
     */
    @Test
    public void testGCPauseCapturing() throws Exception {

        //we try triggering a (non-concurrent) GC with stuff to do
        for (int i = 0; i < 1000000; i++) {
            new ArrayList<>();
        }
        System.gc();
        Thread.sleep(1000);

        ViewData pauseData = viewManager.getView(View.Name.create("jvm/gc/pause"));

        assertThat(pauseData.getAggregationMap()).isNotEmpty();

        Map.Entry<List<TagValue>, AggregationData> minorTime = pauseData.getAggregationMap().entrySet().stream()
                .filter(e -> e.getKey().stream().filter(tag -> tag.asString().contains("minor")).findFirst().isPresent())
                .findFirst().get();

        Map.Entry<List<TagValue>, AggregationData> majorTime = pauseData.getAggregationMap().entrySet().stream()
                .filter(e -> e.getKey().stream().filter(tag -> tag.asString().contains("major")).findFirst().isPresent())
                .findFirst().get();


        assertThat(((AggregationData.SumDataLong) minorTime.getValue()).getSum()).isGreaterThan(0);
        assertThat(((AggregationData.SumDataLong) majorTime.getValue()).getSum()).isGreaterThan(0);
    }
}
