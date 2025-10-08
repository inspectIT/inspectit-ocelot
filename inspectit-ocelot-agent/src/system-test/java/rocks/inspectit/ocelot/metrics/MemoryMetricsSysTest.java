package rocks.inspectit.ocelot.metrics;

import org.junit.jupiter.api.Test;

class MemoryMetricsSysTest extends MetricsSysTestBase {

    /**
     * This test assumes that the JVM was started with a non-concurrent GC
     */
    @Test
    void testMemoryCapturing() {
        assertMetric("jvm_memory_used");
        assertMetric("jvm_memory_committed");
        assertMetric("jvm_memory_max");
        assertMetric("jvm_buffer_count");
        assertMetric("jvm_buffer_memory_used");
        assertMetric("jvm_buffer_total_capacity");
    }
}
