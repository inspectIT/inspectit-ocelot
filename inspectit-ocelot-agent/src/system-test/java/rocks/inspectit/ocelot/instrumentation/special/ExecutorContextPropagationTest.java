package rocks.inspectit.ocelot.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.instrumentation.special.HelperClasses.TestCallable;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ExecutorContextPropagationTest extends InstrumentationSysTestBase {

    private static final Tagger tagger = Tags.getTagger();

    private ExecutorService executorService;

    @BeforeAll
    public static void beforeAll() {
        TestUtils.waitForClassInstrumentations(ThreadPoolExecutor.class);
    }

    @BeforeEach
    public void beforeEach() throws ExecutionException, InterruptedException {
        // warmup the executor - if this is not be done, the first call to the executor will always be correlated
        // because a thread is started, thus, it is correlated due to the Thread.start correlation
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(Math::random).get();
    }

    @Nested
    public class Submit_runnable {

        @Test
        public void correlateRunnable_lambda() throws Exception {
            TagKey tagKey = TagKey.create("tag_key");
            TagValue tagValue = TagValue.create("tag_value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsLambda(refTags);

            Future<?> taskFuture;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                taskFuture = executorService.submit(runnable);
            }
            taskFuture.get();

            assertThat(refTags.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void correlateRunnable_anonymous() throws Exception {
            TagKey tagKey = TagKey.create("tag_key");
            TagValue tagValue = TagValue.create("tag_value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsAnonymous(refTags);

            Future<?> taskFuture;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                taskFuture = executorService.submit(runnable);
            }
            taskFuture.get();

            assertThat(refTags.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void correlateRunnable_named() throws Exception {
            TagKey tagKey = TagKey.create("tag_key");
            TagValue tagValue = TagValue.create("tag_value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsNamed(refTags);

            Future<?> taskFuture;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                taskFuture = executorService.submit(runnable);
            }
            taskFuture.get();

            assertThat(refTags.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }
    }

    @Nested
    public class Execute_runnable {

        @Test
        public void correlateRunnable_lambda() throws Exception {
            TagKey tagKey = TagKey.create("tag_key");
            TagValue tagValue = TagValue.create("tag_value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Runnable runnable = () -> {
                refTags.set(InternalUtils.getTags(Tags.getTagger().getCurrentTagContext()));
                latch.countDown();
            };

            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                executorService.execute(runnable);
            }

            latch.await();

            assertThat(refTags.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void correlateRunnable_anonymous() throws Exception {
            TagKey tagKey = TagKey.create("tag_key");
            TagValue tagValue = TagValue.create("tag_value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    refTags.set(InternalUtils.getTags(Tags.getTagger().getCurrentTagContext()));
                    latch.countDown();
                }
            };

            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                executorService.execute(runnable);
            }

            latch.await();

            assertThat(refTags.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void correlateRunnable_named() throws Exception {
            TagKey tagKey = TagKey.create("tag_key");
            TagValue tagValue = TagValue.create("tag_value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Runnable runnable = new HelperClasses.TestRunnable(unused -> {
                refTags.set(InternalUtils.getTags(Tags.getTagger().getCurrentTagContext()));
                latch.countDown();
            });

            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                executorService.execute(runnable);
            }

            latch.await();

            assertThat(refTags.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }
    }

    @Nested
    public class Submit_callable {

        @Test
        public void submitCallable_lambda() throws Exception {
            TagKey tagKey = TagKey.create("tag_key");
            TagValue tagValue = TagValue.create("tag_value");

            Callable<Iterator<Tag>> callable = HelperClasses.getCallableAsLambda();

            Future<Iterator<Tag>> result;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                result = executorService.submit(callable);
            }

            assertThat(result.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void submitCallable_anonymous() throws Exception {
            TagKey tagKey = TagKey.create("tag_key");
            TagValue tagValue = TagValue.create("tag_value");

            Callable<Iterator<Tag>> callable = HelperClasses.getCallableAsAnonymous();

            Future<Iterator<Tag>> result;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                result = executorService.submit(callable);
            }

            assertThat(result.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void submitCallable_named() throws Exception {
            TagKey tagKey = TagKey.create("tag_key");
            TagValue tagValue = TagValue.create("tag_value");

            Callable<Iterator<Tag>> callable = HelperClasses.getCallableAsNamed();

            Future<Iterator<Tag>> result;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                result = executorService.submit(callable);
            }

            assertThat(result.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }
    }
}
