package rocks.inspectit.ocelot.metrics;

import org.junit.jupiter.api.BeforeEach;
import rocks.inspectit.ocelot.utils.TestUtils;

public class MetricsSysTestBase {

    @BeforeEach
    void flushMetrics() {
        TestUtils.waitForOpenCensusQueueToBeProcessed();
    }

}
