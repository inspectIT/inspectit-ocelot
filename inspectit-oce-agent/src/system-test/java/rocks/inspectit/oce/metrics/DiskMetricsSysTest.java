package rocks.inspectit.oce.metrics;

import io.opencensus.stats.Stats;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewData;
import io.opencensus.stats.ViewManager;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.ConfigAlteringSysTest;

import static org.assertj.core.api.Assertions.assertThat;

public class DiskMetricsSysTest extends ConfigAlteringSysTest {

    //acquire the impl for clearing recorded stats for test purposes
    private static final ViewManager viewManager = Stats.getViewManager();

    @Test
    public void testDiskMetricCapturing() throws Exception {

        //test the master switch
        setProperties(
                "inspectit.metrics.disk.free", "true",
                "inspectit.metrics.disk.total", "true",
                "inspectit.metrics.enabled", "false"
        );
        Thread.sleep(1000);
        ViewData freeData = viewManager.getView(View.Name.create("disk/free"));
        ViewData totalData = viewManager.getView(View.Name.create("disk/total"));
        assertThat(freeData).isNull();
        assertThat(totalData).isNull();

        //flip off the master switch, turn on only free space monitoring
        setProperties(
                "inspectit.metrics.disk.total", "false",
                "inspectit.metrics.enabled", "true"
        );
        Thread.sleep(1000);
        freeData = viewManager.getView(View.Name.create("disk/free"));
        totalData = viewManager.getView(View.Name.create("disk/total"));
        assertThat(freeData.getAggregationMap()).isNotEmpty();
        assertThat(totalData).isNull();

        //turn on both
        setProperties(
                "inspectit.metrics.disk.total", "true"
        );
        Thread.sleep(1000);
        freeData = viewManager.getView(View.Name.create("disk/free"));
        totalData = viewManager.getView(View.Name.create("disk/total"));
        assertThat(freeData.getAggregationMap()).isNotEmpty();
        assertThat(totalData.getAggregationMap()).isNotEmpty();
    }
}
