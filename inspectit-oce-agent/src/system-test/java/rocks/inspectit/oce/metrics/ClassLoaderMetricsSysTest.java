package rocks.inspectit.oce.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassLoaderMetricsSysTest extends MetricsSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    @Test
    public void testClassLoaderMetricCapturing() throws Exception {
        ViewData loadedData = viewManager.getView(View.Name.create("jvm/classes/loaded"));
        ViewData unloadedData = viewManager.getView(View.Name.create("jvm/classes/unloaded"));
        assertThat(loadedData.getAggregationMap()).isNotEmpty();
        assertThat(unloadedData.getAggregationMap()).isNotEmpty();

        Map.Entry<List<TagValue>, AggregationData> loaded = loadedData.getAggregationMap().entrySet().stream().findFirst().get();
        Map.Entry<List<TagValue>, AggregationData> unloaded = unloadedData.getAggregationMap().entrySet().stream().findFirst().get();

        //ensure that tags are present
        assertThat(loaded.getKey()).isNotEmpty();
        assertThat(unloaded.getKey()).isNotEmpty();

        //ensure that the values are sane
        long loadedVal = ((AggregationData.LastValueDataLong) loaded.getValue()).getLastValue();
        long unloadedVal = ((AggregationData.LastValueDataLong) unloaded.getValue()).getLastValue();

        assertThat(loadedVal).isLessThanOrEqualTo(ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
        assertThat(unloadedVal).isLessThanOrEqualTo(ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount());
    }
}
