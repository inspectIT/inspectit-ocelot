package rocks.inspectit.oce.metrics;

import org.junit.jupiter.api.BeforeEach;
import rocks.inspectit.oce.utils.TestUtils;

public class MetricsSysTestBase {

    @BeforeEach
    void flushMetrics() {
        TestUtils.waitForOpenCensusQueueToBeProcessed();
    }

}
