package rocks.inspectit.ocelot.bootstrap.correlation.noop;

import rocks.inspectit.ocelot.bootstrap.correlation.LogTraceCorrelator;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class NoopLogTraceCorrelator implements LogTraceCorrelator {

    public static final LogTraceCorrelator INSTANCE = new NoopLogTraceCorrelator();

    @Override
    public AutoCloseable startCorrelatedSpanScope(Supplier<? extends AutoCloseable> spanScopeStarter) {
        return spanScopeStarter.get();
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        return runnable;
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        return callable;
    }

    @Override
    public AutoCloseable applyCorrelationToMDC() {
        return () -> {
        };
    }
}
