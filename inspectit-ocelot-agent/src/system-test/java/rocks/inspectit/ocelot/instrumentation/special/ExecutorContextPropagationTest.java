package rocks.inspectit.ocelot.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutorContextPropagationTest extends InstrumentationSysTestBase {

    private static final Tagger tagger = Tags.getTagger();

    @Test
    public void testContextPropagationAcrossExecutorForRunnables() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(2);
        TagKey keyToPropagate = TagKey.create("propagation/test/tag");

        AtomicReference<String> tagValue = new AtomicReference<>(null);

        Future<?> taskFuture;

        try (Scope s = tagger.currentBuilder().putLocal(keyToPropagate, TagValue.create("myval")).buildScoped()) {
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
        try (Scope s = tagger.currentBuilder().putLocal(keyToPropagate, TagValue.create("myval")).buildScoped()) {
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
