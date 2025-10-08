package rocks.inspectit.ocelot.metrics;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class MemoryMetricsSysTest extends MetricsSysTestBase {

    /**
     * This test assumes that the JVM was started with a non-concurrent GC
     */
    @Test
    public void testMemoryCapturing() {
        assertMetric("jvm_memory_used");
        assertMetric("jvm_memory_committed");
        assertMetric("jvm_memory_max");
        assertMetric("jvm_buffer_count");
        assertMetric("jvm_buffer_memory_used");
        assertMetric("jvm_buffer_total_capacity");
    }
}
