package rocks.inspectit.ocelot.instrumentation.special;

import io.opentelemetry.api.baggage.Baggage;
import org.apache.commons.collections.Bag;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public final class HelperClasses {

    // No instance of this class please
    private HelperClasses() {
    }

    public static class TestRunnable implements Runnable {

        private final Consumer<Void> callback;

        public TestRunnable(Consumer<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            callback.accept(null);
        }
    }

    public static class TestCallable<T> implements Callable<T> {

        private final Function<Void, T> callback;

        public TestCallable(Function<Void, T> callback) {
            this.callback = callback;
        }

        @Override
        public T call() throws Exception {
            return callback.apply(null);
        }
    }

    public static Runnable getRunnableAsLambda(AtomicReference<Baggage> refBagage) {
        return () -> refBagage.set(Baggage.current());
    }

    public static Runnable getRunnableAsAnonymous(AtomicReference<Baggage> refBaggage) {
        return new Runnable() {
            @Override
            public void run() {
                refBaggage.set(Baggage.current());
            }
        };
    }

    public static Runnable getRunnableAsNamed(AtomicReference<Baggage> refBaggage) {
        return new TestRunnable(unused -> refBaggage.set(Baggage.current()));
    }

    public static Callable<Baggage> getCallableAsLambda() {
        return () -> Baggage.current();
    }

    public static Callable<Baggage> getCallableAsAnonymous() {
        return new Callable<Baggage>() {
            @Override
            public Baggage call(){
                return Baggage.current();
            }
        };
    }

    public static Callable<Baggage> getCallableAsNamed() {
        return new TestCallable<Baggage>((unused) -> Baggage.current());
    }
}
