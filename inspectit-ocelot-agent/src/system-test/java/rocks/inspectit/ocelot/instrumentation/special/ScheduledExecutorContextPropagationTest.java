package rocks.inspectit.ocelot.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.Tag;
import io.opencensus.tags.Tags;
import io.opencensus.tags.*;
import org.junit.jupiter.api.*;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.instrumentation.special.HelperClasses.TestCallable;
import rocks.inspectit.ocelot.instrumentation.special.HelperClasses.TestRunnable;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ScheduledExecutorContextPropagationTest extends InstrumentationSysTestBase {

    private static final Tagger tagger = Tags.getTagger();

    private ScheduledExecutorService executorService;

    @BeforeAll
    public static void beforeAll() {
        Executors.newSingleThreadScheduledExecutor().schedule(Math::random, 1, TimeUnit.MILLISECONDS);
        TestUtils.waitForClassInstrumentations(ScheduledThreadPoolExecutor.class, TestRunnable.class, TestCallable.class);
    }

    @BeforeEach
    public void beforeEach() throws ExecutionException, InterruptedException {
        // warmup the executor - if this is not be done, the first call to the executor will always be correlated
        // because a thread is started, thus, it is correlated due to the Thread.start correlation
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.submit(Math::random).get();
    }

    @AfterEach
    public void afterEach() {
        executorService.shutdown();
    }

    @Nested
    public class Schedule_runnable {

        @Test
        public void verifyCtxPropagationViaScheduleRunnable_lambda() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsLambda(refTags);

            ScheduledFuture<?> schedule;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
            }
            schedule.get();

            assertThat(refTags.get()).toIterable().hasSize(1).extracting("key", "value").contains(tuple(tagKey, tagValue));
        }

        @Test
        public void verifyCtxPropagationViaScheduleRunnable_anonymous() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsAnonymous(refTags);
            TestUtils.waitForClassInstrumentations(runnable.getClass());

            ScheduledFuture<?> schedule;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
            }
            schedule.get();

            assertThat(refTags.get()).toIterable().hasSize(1).extracting("key", "value").contains(tuple(tagKey, tagValue));
        }

        @Test
        public void verifyCtxPropagationViaScheduleRunnable_named() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsNamed(refTags);

            ScheduledFuture<?> schedule;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
            }
            schedule.get();

            assertThat(refTags.get()).toIterable().hasSize(1).extracting("key", "value").contains(tuple(tagKey, tagValue));
        }
    }

    @Nested
    public class Schedule_callable {

        @Test
        public void verifyCtxPropagationViaScheduleCallable_lambda() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");

            Callable<Iterator<Tag>> callable = HelperClasses.getCallableAsLambda();

            ScheduledFuture<Iterator<Tag>> future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
            }
            Iterator<Tag> result = future.get();

            assertThat(result).toIterable().hasSize(1).extracting("key", "value").contains(tuple(tagKey, tagValue));
        }

        @Test
        public void verifyCtxPropagationViaScheduleCallable_anonymous() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");

            Callable<Iterator<Tag>> callable = HelperClasses.getCallableAsAnonymous();
            TestUtils.waitForClassInstrumentations(callable.getClass());

            ScheduledFuture<Iterator<Tag>> future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
            }
            Iterator<Tag> result = future.get();

            assertThat(result).toIterable().hasSize(1).extracting("key", "value").contains(tuple(tagKey, tagValue));
        }

        @Test
        public void verifyCtxPropagationViaScheduleCallable_named() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");

            Callable<Iterator<Tag>> callable = HelperClasses.getCallableAsNamed();

            ScheduledFuture<Iterator<Tag>> future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
            }
            Iterator<Tag> result = future.get();

            assertThat(result).toIterable().hasSize(1).extracting("key", "value").contains(tuple(tagKey, tagValue));
        }
    }

    @Nested
    public class ScheduleWithFixedDelay {

        @Test
        public void verifyCtxPropagationViaScheduleFixedDelay_lambda() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = () -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                iteratorList.add(iter);
                interationCount.countDown();
            };

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator).toIterable().hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedDelay_anonymous() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                    iteratorList.add(iter);
                    interationCount.countDown();
                }
            };
            TestUtils.waitForClassInstrumentations(runnable.getClass());

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator).toIterable().hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedDelay_named() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = new TestRunnable(unused -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                iteratorList.add(iter);
                interationCount.countDown();
            });
            TestUtils.waitForClassInstrumentations(runnable.getClass());

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator).toIterable().hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }
    }

    @Nested
    public class ScheduleAtFixedRate {

        @Test
        public void verifyCtxPropagationViaScheduleFixedRate_lambda() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = () -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                iteratorList.add(iter);
                interationCount.countDown();
            };

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator).toIterable().hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedRate_anonymous() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                    iteratorList.add(iter);
                    interationCount.countDown();
                }
            };
            TestUtils.waitForClassInstrumentations(runnable.getClass()); // wait for anonymous class instrumentation

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator).toIterable().hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedRate_named() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = new TestRunnable(unused -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                iteratorList.add(iter);
                interationCount.countDown();
            });

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator).toIterable().hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }
    }
}
