package rocks.inspectit.ocelot.core.instrumentation.context;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.context.session.PropagationSessionStorage;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ContextManagerPerfTest {

    private ContextManager myContextManager;

    private Thread thread;

    @Setup
    public void init() {
        CommonAttributesManager commonAttributesManager = new CommonAttributesManager();
        InstrumentationConfigurationResolver configurationResolver = new InstrumentationConfigurationResolver();
        PropagationSessionStorage sessionStorage = new PropagationSessionStorage();

        myContextManager = new ContextManager(commonAttributesManager, sessionStorage, configurationResolver);
        thread = Thread.currentThread();
    }

    @Benchmark
    public void wrapRunnable(Blackhole blackhole) {
        blackhole.consume(myContextManager.wrap(() -> {
        }));
    }

    @Benchmark
    public void wrapCallable(Blackhole blackhole) {
        blackhole.consume(myContextManager.wrap(() -> "test"));
    }

}
