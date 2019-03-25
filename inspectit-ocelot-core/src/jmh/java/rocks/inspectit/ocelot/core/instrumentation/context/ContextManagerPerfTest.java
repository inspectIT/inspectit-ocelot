package rocks.inspectit.ocelot.core.instrumentation.context;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ContextManagerPerfTest {

    private ContextManager myContextManager;

    private Thread thread;

    @Setup
    public void init() {
        CommonTagsManager commonTagsManager = new CommonTagsManager();
        InstrumentationConfigurationResolver configurationResolver = new InstrumentationConfigurationResolver();

        myContextManager = new ContextManager(commonTagsManager, configurationResolver);
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

    @Benchmark
    public void storeAndAttachContext() {
        myContextManager.storeContextForThread(thread);
        myContextManager.attachContextToThread(thread);
    }

}
