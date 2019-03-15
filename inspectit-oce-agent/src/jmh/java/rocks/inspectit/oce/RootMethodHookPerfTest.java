package rocks.inspectit.oce;

import org.openjdk.jmh.annotations.*;
import rocks.inspectit.oce.target.MethodHookTarget;
import rocks.inspectit.oce.utils.TestUtils;

import java.util.concurrent.TimeUnit;

/**
 * Performs method hook measurements for the methods that are not inside of an already started inspectIT context.
 * <p>
 * The performance here relates to the methods that are  considered as the entry points in the application.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class RootMethodHookPerfTest {

    private MethodHookTarget target = new MethodHookTarget();

    @Setup
    public void waitForInstrumentation() {
        TestUtils.waitForInstrumentationToComplete();
    }

    /**
     * Not instrumented.
     */
    @Benchmark
    public void baseline() {
        target.methodNotInstrumented();
    }

    /**
     * Tests an empty hook.
     */
    @Benchmark
    public void methodNoAction() {
        target.methodNoAction();
    }

    /**
     * Test the hook that provides method start time and duration to the inspectIT Context.
     */
    @Benchmark
    public void methodResponseTime() {
        target.methodResponseTime();
    }

}
