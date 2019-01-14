package rocks.inspectit.oce.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutorContextPropagationTest {

    private static final Tagger tagger = Tags.getTagger();

    @Test
    public void testContextPropagationAcrossExecutor() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(2);
        TagKey keyToPropagate = TagKey.create("propagation/test/tag");

        AtomicBoolean taskFinished = new AtomicBoolean(false);
        AtomicReference<String> tagValue = new AtomicReference<>(null);
        Object monitor = new Object();

        try (Scope s = tagger.currentBuilder().put(keyToPropagate, TagValue.create("myval")).buildScoped()) {
            es.execute(() -> {
                Iterator<Tag> it = InternalUtils.getTags(tagger.getCurrentTagContext());
                while (it.hasNext()) {
                    Tag tag = it.next();
                    if (tag.getKey().equals(keyToPropagate)) {
                        tagValue.set(tag.getValue().asString());
                    }
                }
                taskFinished.set(true);
                synchronized (monitor) {
                    monitor.notify();
                }
            });
        }
        synchronized (monitor) {
            while (!taskFinished.get()) {
                monitor.wait();
            }
        }
        assertThat(tagValue.get()).isEqualTo("myval");

    }

}
