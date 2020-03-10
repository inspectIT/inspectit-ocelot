package rocks.inspectit.ocelot.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class JmxMetricsSysTest extends MetricsSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    @Test
    public void uptime() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData uptimeViewData = viewManager.getView(View.Name.create("jvm/jmx/java/lang/Runtime/Uptime"));

            assertThat(uptimeViewData).isNotNull();
            assertThat(uptimeViewData.getAggregationMap()).isNotEmpty();

            Map.Entry<List<TagValue>, AggregationData> uptime = uptimeViewData.getAggregationMap().entrySet().stream().findFirst().get();

            //ensure that tags are present
            assertThat(uptime.getKey()).isNotEmpty();

            //ensure that the values are sane
            double uptimeVal = ((AggregationData.LastValueDataDouble) uptime.getValue()).getLastValue();

            assertThat(uptimeVal).isLessThanOrEqualTo(ManagementFactory.getRuntimeMXBean().getUptime());
        });
    }

}
