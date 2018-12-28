package rocks.inspectit.oce.metrics;

import io.opencensus.stats.Stats;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewData;
import io.opencensus.stats.ViewManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DiskMetricsSysTest {

    //acquire the impl for clearing recorded stats for test purposes
    private static final ViewManager viewManager = Stats.getViewManager();

    @Test
    public void testDiskMetricCapturing() throws Exception {
        ViewData freeData = viewManager.getView(View.Name.create("disk/free"));
        ViewData totalData = viewManager.getView(View.Name.create("disk/total"));
        assertThat(freeData.getAggregationMap()).isNotEmpty();
        assertThat(totalData.getAggregationMap()).isNotEmpty();
    }
}
