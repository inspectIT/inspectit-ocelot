package rocks.inspectit.oce.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ScheduledExecutorContextPropagationTest {

    private static final Tagger tagger = Tags.getTagger();

    @Test
    public void verifyCtxPropagationViaScheduleRunnable() throws Exception {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

        Runnable runnable = () -> {
            Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
            refTags.set(iter);

            synchronized (refTags) {
                refTags.notifyAll();
            }
        };

        ScheduledFuture<?> schedule;
        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            schedule = executorService.schedule(runnable, 50, TimeUnit.MILLISECONDS);
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
        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            future = executorService.schedule(callable, 50, TimeUnit.MILLISECONDS);
        }

        assertThat(future.get()).hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue));
    }

    @Test
    public void verifyCtxPropagationViaScheduleFixedDelay() throws Exception {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        int executions = 5;

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

        Runnable runnable = () -> {
            Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());

            iteratorList.add(iter);

            synchronized (iteratorList) {
                iteratorList.notifyAll();
            }
        };

        ScheduledFuture<Object> future;
        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            future = (ScheduledFuture<Object>) executorService.scheduleWithFixedDelay(runnable, 0, 50, TimeUnit.MILLISECONDS);
        }

        synchronized (iteratorList) {
            while (iteratorList.size() < executions) {
                iteratorList.wait();
            }
        }

        future.cancel(true);
        executorService.shutdown();

        assertThat(iteratorList).hasSize(executions);
        iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                .hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue)));
    }

    @Test
    public void verifyCtxPropagationViaScheduleFixedRate() throws Exception {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        int executions = 5;

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

        Runnable runnable = () -> {
            Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());

            iteratorList.add(iter);

            synchronized (iteratorList) {
                iteratorList.notifyAll();
            }
        };

        ScheduledFuture<Object> future;
        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            future = (ScheduledFuture<Object>) executorService.scheduleAtFixedRate(runnable, 0, 50, TimeUnit.MILLISECONDS);
        }

        synchronized (iteratorList) {
            while (iteratorList.size() < executions) {
                iteratorList.wait();
            }
        }

        future.cancel(true);
        executorService.shutdown();

        assertThat(iteratorList).hasSize(executions);
        iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                .hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue)));
    }
}
