package rocks.inspectit.ocelot.metrics;

import com.sun.management.GarbageCollectionNotificationInfo;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.PointData;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import rocks.inspectit.ocelot.utils.MetricTestUtils;

import javax.management.NotificationEmitter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class GCMetricsSysTest extends MetricsSysTestBase {

    private static final Logger log = LoggerFactory.getLogger(GCMetricsSysTest.class);

    public static List blackhole;

    /**
     * This test assumes that the JVM was started with a non-concurrent GC
     */
    @Test
    public void testGCPauseCapturing() throws Exception {
        // we try triggering a (non-concurrent) GC with stuff to do
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

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            Collection<PointData> gcData = MetricTestUtils.getAllDataForView("jvm_gc_pause");

            assertThat(gcData).isNotEmpty();
            assertThat(gcData.stream().findFirst().get().getAttributes().asMap()).isNotEmpty();

            Optional<PointData> gcMinor = gcData.stream()
                    .filter(point -> containsStringInAttributeValue(point, "minor"))
                    .findFirst();
            Optional<PointData> gcMajor = gcData.stream()
                    .filter(point -> containsStringInAttributeValue(point, "major"))
                    .findFirst();

            assertThat(gcMinor).isNotEmpty();
            assertThat(gcMajor).isNotEmpty();

            LongPointData gcMinorLong = (LongPointData) gcMinor.get();
            LongPointData gcMajorLong = (LongPointData) gcMajor.get();

            assertThat(gcMinorLong.getValue()).isGreaterThanOrEqualTo(0);
            assertThat(gcMajorLong.getValue()).isGreaterThan(0);
        });
    }

    /**
     * @return true, if the point data contains an attribute value with the searched string
     */
    private static boolean containsStringInAttributeValue(PointData pointData, String search) {
        for (Object value : pointData.getAttributes().asMap().values()) {
            String valueString = value.toString();
            if (valueString.contains(search)) return true;
        }
        return false;
    }
}
