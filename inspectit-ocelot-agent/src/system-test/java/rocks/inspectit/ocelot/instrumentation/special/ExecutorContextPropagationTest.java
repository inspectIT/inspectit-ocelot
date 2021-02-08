package rocks.inspectit.ocelot.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    public void testContextPropagationAcrossExecutorForRunnables() throws Exception {
        TagKey keyToPropagate = TagKey.create("propagation/test/tag");

        AtomicReference<String> tagValue = new AtomicReference<>(null);

        Future<?> taskFuture;

        try (Scope s = tagger.currentBuilder().putLocal(keyToPropagate, TagValue.create("myval")).buildScoped()) {
            taskFuture = executorService.submit(() -> {
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
        TagKey keyToPropagate = TagKey.create("propagation/test/tag");


        Future<String> taskFuture;
        try (Scope s = tagger.currentBuilder().putLocal(keyToPropagate, TagValue.create("myval")).buildScoped()) {
            taskFuture = executorService.submit(() -> {
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
