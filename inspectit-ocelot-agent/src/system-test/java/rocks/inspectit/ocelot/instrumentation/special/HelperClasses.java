package rocks.inspectit.ocelot.instrumentation.special;

import io.opencensus.tags.InternalUtils;
import io.opencensus.tags.Tag;
import io.opencensus.tags.Tags;

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

    public static Runnable getRunnableAsLambda(AtomicReference<Iterator<Tag>> refTags) {
        return () -> {
            refTags.set(InternalUtils.getTags(Tags.getTagger().getCurrentTagContext()));
        };
    }

    public static Runnable getRunnableAsAnonymous(AtomicReference<Iterator<Tag>> refTags) {
        return new Runnable() {
            @Override
            public void run() {
                refTags.set(InternalUtils.getTags(Tags.getTagger().getCurrentTagContext()));
            }
        };
    }

    public static Runnable getRunnableAsNamed(AtomicReference<Iterator<Tag>> refTags) {
        return new TestRunnable(unused -> {
            refTags.set(InternalUtils.getTags(Tags.getTagger().getCurrentTagContext()));
        });
    }

    public static Callable<Iterator<Tag>> getCallableAsLambda() {
        return () -> InternalUtils.getTags(Tags.getTagger().getCurrentTagContext());
    }

    public static Callable<Iterator<Tag>> getCallableAsAnonymous() {
        return new Callable<Iterator<Tag>>() {
            @Override
            public Iterator<Tag> call() throws Exception {
                return InternalUtils.getTags(Tags.getTagger().getCurrentTagContext());
            }
        };
    }

    public static Callable<Iterator<Tag>> getCallableAsNamed() {
        return new TestCallable<Iterator<Tag>>((unused) -> InternalUtils.getTags(Tags.getTagger().getCurrentTagContext()));
    }
}
