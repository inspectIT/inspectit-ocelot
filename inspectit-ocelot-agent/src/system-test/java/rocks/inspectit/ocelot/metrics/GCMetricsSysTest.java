package rocks.inspectit.ocelot.metrics;

import com.sun.management.GarbageCollectionNotificationInfo;
import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import javax.management.NotificationEmitter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class GCMetricsSysTest extends MetricsSysTestBase {

    private static final Logger log = LoggerFactory.getLogger(GCMetricsSysTest.class);

    private static final ViewManager viewManager = Stats.getViewManager();

    public static List blackhole;

    /**
     * This test assumes that the JVM was started with a non-concurrent GC
     */
    @Test
    public void testGCPauseCapturing() throws Exception {
        //we try triggering a (non-concurrent) GC with stuff to do
        for (int i = 0; i < 1000000; i++) {
            blackhole = new ArrayList<>();
        }

        AtomicBoolean gcOccurred = new AtomicBoolean(false);
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (mbean instanceof NotificationEmitter) {
                ((NotificationEmitter) mbean).addNotificationListener((not, hb) -> {
                    if (not.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                        gcOccurred.set(true);
                    }
                }, null, null);
            }
        }
        int tries = 0;
        while (!gcOccurred.get() && tries < 10000) {
            System.gc();
            System.runFinalization();

            // even more trash
            for (int x = 0; x < 1000000; x++) {
                blackhole = new ArrayList<>();
            }

            Thread.sleep(10);
            tries++;
        }

        if (!gcOccurred.get()) {
            log.warn(() -> "Unable to trigger a GC in time! Aborting test");
            return;
        }

        //we need to wait for the GC events to be fired and handled
        Thread.sleep(500);
        flushMetrics();

        ViewData pauseData = viewManager.getView(View.Name.create("jvm/gc/pause"));

        assertThat(pauseData.getAggregationMap()).isNotEmpty();

        Map.Entry<List<TagValue>, AggregationData> minorTime = pauseData.getAggregationMap()
                .entrySet()
                .stream()
                .filter(e -> e.getKey()
                        .stream()
                        .filter(tag -> tag.asString().contains("minor"))
                        .findFirst()
                        .isPresent())
                .findFirst()
                .get();

        Map.Entry<List<TagValue>, AggregationData> majorTime = pauseData.getAggregationMap()
                .entrySet()
                .stream()
                .filter(e -> e.getKey()
                        .stream()
                        .filter(tag -> tag.asString().contains("major"))
                        .findFirst()
                        .isPresent())
                .findFirst()
                .get();

        assertThat(((AggregationData.SumDataLong) minorTime.getValue()).getSum()).isGreaterThanOrEqualTo(0);
        assertThat(((AggregationData.SumDataLong) majorTime.getValue()).getSum()).isGreaterThan(0);
    }
}
