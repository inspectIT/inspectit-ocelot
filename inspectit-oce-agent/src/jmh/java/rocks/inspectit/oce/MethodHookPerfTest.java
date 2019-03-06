package rocks.inspectit.oce;

import org.openjdk.jmh.annotations.*;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.bootstrap.context.IInspectitContext;
import rocks.inspectit.oce.target.MethodHookTarget;
import rocks.inspectit.oce.utils.TestUtils;

import java.util.concurrent.TimeUnit;

/**
 * Performs method hook measurements for the methods that are inside of the already existing inspectIT context.
 * <p>
 * The performance here relates to the methods that are not considered as the entry points in the application.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class MethodHookPerfTest {

    private MethodHookTarget target = new MethodHookTarget();

    private IInspectitContext root;

    @Setup
    public void waitForInstrumentation() {
        TestUtils.waitForInstrumentationToComplete();
    }

    @Setup(Level.Iteration)
    public void createRootContext() {
        root = Instances.contextManager.enterNewContext();
        root.makeActive();
    }

    @TearDown(Level.Iteration)
    public void closeRootContext() {
        root.close();
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
