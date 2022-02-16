package rocks.inspectit.ocelot.instrumentation.hook;

import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class RecursionInstrumentation extends InstrumentationSysTestBase {

    private static AtomicInteger invocationCount = new AtomicInteger(0);

    /**
     * This method is instrumentation and will be called in the instrumentation itself
     * which would result in endless loop and StackOverflowError.
     */
    public static void helloWorld() {
        System.out.println("Hello World!");
        invocationCount.incrementAndGet();
    }

    @Test
    public void recursiveInstrumentation() {
        TestUtils.waitForClassInstrumentations(RecursionInstrumentation.class);

        helloWorld();

        // two calls - one of the application and one (only!) due to the instrumentation
        assertThat(invocationCount.get()).isEqualTo(2);
    }

}
