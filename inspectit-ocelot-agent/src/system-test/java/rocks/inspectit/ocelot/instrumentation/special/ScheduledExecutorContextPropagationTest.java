package rocks.inspectit.ocelot.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ScheduledExecutorContextPropagationTest extends InstrumentationSysTestBase {

    private static final Tagger tagger = Tags.getTagger();

    @Test
    public void verifyCtxPropagationViaScheduleRunnable() throws Exception {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

        Runnable runnable = () -> refTags.set(InternalUtils.getTags(tagger.getCurrentTagContext()));

        ScheduledFuture<?> schedule;
        try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
            schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
        }
        schedule.get();

        assertThat(refTags.get()).hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue));
    }

    @Test
    public void verifyCtxPropagationViaScheduleCallable() throws Exception {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        Callable<Iterator<Tag>> callable = () -> InternalUtils.getTags(tagger.getCurrentTagContext());

        ScheduledFuture<Iterator<Tag>> future;
        try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
            future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
        }
        Iterator<Tag> result = future.get();

        assertThat(result).hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue));
    }

    @Test
    public void verifyCtxPropagationViaScheduleFixedDelay() throws Exception {
        int iterations = 5;
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        CountDownLatch interationCount = new CountDownLatch(iterations);

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
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
        iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                .hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue)));
    }

    @Test
    public void verifyCtxPropagationViaScheduleFixedRate() throws Exception {
        int iterations = 5;
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        CountDownLatch interationCount = new CountDownLatch(iterations);

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
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
        iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                .hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue)));
    }
}
