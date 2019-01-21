package rocks.inspectit.oce.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutorContextPropagationTest {

    private static final Tagger tagger = Tags.getTagger();

    @Test
    public void testContextPropagationAcrossExecutorForRunnables() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(2);
        TagKey keyToPropagate = TagKey.create("propagation/test/tag");

        AtomicReference<String> tagValue = new AtomicReference<>(null);

        Future<?> taskFuture;

        //TODO: wait until the instrumentation is complete as soon as selfmonitoring is added to monitor the instrumentation queues
        Thread.sleep(5000);

        try (Scope s = tagger.currentBuilder().put(keyToPropagate, TagValue.create("myval")).buildScoped()) {
            taskFuture = es.submit(() -> {
                Iterator<Tag> it = InternalUtils.getTags(tagger.getCurrentTagContext());
                while (it.hasNext()) {
                    Tag tag = it.next();
                    if (tag.getKey().equals(keyToPropagate)) {
                        tagValue.set(tag.getValue().asString());
                    }
                }
            });
        }
        taskFuture.get();
        assertThat(tagValue.get()).isEqualTo("myval");

    }


    @Test
    public void testContextPropagationAcrossExecutorForCallables() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(2);
        TagKey keyToPropagate = TagKey.create("propagation/test/tag");


        Future<String> taskFuture;

        //TODO: wait until the instrumentation is complete as soon as selfmonitoring is added to monitor the instrumentation queues
        Thread.sleep(5000);

        try (Scope s = tagger.currentBuilder().put(keyToPropagate, TagValue.create("myval")).buildScoped()) {
            taskFuture = es.submit(() -> {
                Iterator<Tag> it = InternalUtils.getTags(tagger.getCurrentTagContext());
                while (it.hasNext()) {
                    Tag tag = it.next();
                    if (tag.getKey().equals(keyToPropagate)) {
                        return tag.getValue().asString();
                    }
                }
                return null;
            });
        }

        assertThat(taskFuture.get()).isEqualTo("myval");

    }

}
