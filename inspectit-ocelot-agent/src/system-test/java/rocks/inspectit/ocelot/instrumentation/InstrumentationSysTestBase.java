package rocks.inspectit.ocelot.instrumentation;

import org.junit.jupiter.api.BeforeAll;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

/**
 * Base class for every test using instrumentation or metrics
 */
public class InstrumentationSysTestBase {

    @BeforeAll
    static void waitForInstrumentation() {
        TestUtils.waitForAgentInitialization();
        MetricTestUtils.initializeMetricReader();
        TestUtils.waitForInstrumentationToComplete();
    }
}
