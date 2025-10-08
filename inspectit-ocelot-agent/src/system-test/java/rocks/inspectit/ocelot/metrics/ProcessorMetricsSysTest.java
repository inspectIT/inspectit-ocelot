package rocks.inspectit.ocelot.metrics;

import org.junit.jupiter.api.Test;

public class ProcessorMetricsSysTest extends MetricsSysTestBase {

    @Test
    public void testProcessorCountCapturing() {
        assertMetric("system_cpu_count", Runtime.getRuntime().availableProcessors());
    }
}
