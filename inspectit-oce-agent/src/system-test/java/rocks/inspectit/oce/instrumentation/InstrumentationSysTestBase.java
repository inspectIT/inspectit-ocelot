package rocks.inspectit.oce.instrumentation;

import org.junit.jupiter.api.BeforeAll;

import static rocks.inspectit.oce.TestUtils.waitForInstrumentationToComplete;

public class InstrumentationSysTestBase {

    @BeforeAll
    static void waitForInstrumentation() {
        waitForInstrumentationToComplete();
    }
}
