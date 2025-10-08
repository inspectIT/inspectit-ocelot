package rocks.inspectit.ocelot.metrics;

import org.junit.jupiter.api.Test;

class ProcessorMetricsSysTest extends MetricsSysTestBase {

    @Test
    void testProcessorCountCapturing() {
        assertMetric("system_cpu_count", Runtime.getRuntime().availableProcessors());
    }
}
