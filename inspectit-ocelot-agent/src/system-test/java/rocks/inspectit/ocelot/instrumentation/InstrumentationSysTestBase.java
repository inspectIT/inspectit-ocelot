package rocks.inspectit.ocelot.instrumentation;

import org.junit.jupiter.api.BeforeAll;
import rocks.inspectit.ocelot.utils.TestUtils;

public class InstrumentationSysTestBase {

    @BeforeAll
    static void waitForInstrumentation() {
        TestUtils.waitForAgentInitialization();
        TestUtils.waitForInstrumentationToComplete();
    }
}
