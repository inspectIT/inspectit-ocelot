package rocks.inspectit.ocelot.metrics;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricTestUtils;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class JmxMetricsSysTest extends MetricsSysTestBase {

    @Test
    void uptime() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            LongPointData uptimeData = (LongPointData) MetricTestUtils.getDataForView("jvm_jmx_java_lang_runtime_uptime", emptyMap());

            assertThat(uptimeData).isNotNull();

            // ensure that attributes are present
            assertThat(uptimeData.getAttributes().asMap()).isNotEmpty();

            // ensure that the values are sane
            long loaded = uptimeData.getValue();

            assertThat(loaded).isLessThanOrEqualTo(ManagementFactory.getRuntimeMXBean().getUptime());
        });
    }
}
