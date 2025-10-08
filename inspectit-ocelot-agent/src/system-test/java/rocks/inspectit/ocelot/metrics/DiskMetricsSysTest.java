package rocks.inspectit.ocelot.metrics;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class DiskMetricsSysTest extends MetricsSysTestBase {

    @Test
    void testDiskMetricCapturing() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {

            LongPointData freeDisk = (LongPointData) MetricTestUtils.getDataForView("disk_free", emptyMap());
            LongPointData totalDisk = (LongPointData) MetricTestUtils.getDataForView("disk_total", emptyMap());

            assertThat(freeDisk).isNotNull();
            assertThat(totalDisk).isNotNull();

            // ensure that attributes are present
            assertThat(freeDisk.getAttributes().asMap()).isNotEmpty();
            assertThat(totalDisk.getAttributes().asMap()).isNotEmpty();

            // ensure that the values are sane
            long freeVal = freeDisk.getValue();
            long totalVal = totalDisk.getValue();

            assertThat(freeVal).isGreaterThanOrEqualTo(0);
            assertThat(freeVal).isLessThan(totalVal);
        });
    }
}
