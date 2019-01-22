package rocks.inspectit.oce.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ThreadStartContextPropagationTest {

    private static final Tagger tagger = Tags.getTagger();

    @Test
    public void verifyContextProgapation() throws InterruptedException {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");

        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
            refTags.set(iter);

            synchronized (refTags) {
                refTags.notifyAll();
            }
        });

        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            thread.start();
        }

        synchronized (refTags) {
            while (refTags.get() == null) {
                refTags.wait();
            }
        }

        assertThat(refTags.get()).hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue));
    }

    @Test
    public void verifyContextProgapationUsingSubClasses() throws InterruptedException {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");

        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
            refTags.set(iter);

            synchronized (refTags) {
                refTags.notifyAll();
            }
        }) {
            @Override
            public synchronized void start() {
                System.out.println("Custom Implementation");
                super.start();
            }
        };

        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            thread.start();
        }

        synchronized (refTags) {
            while (refTags.get() == null) {
                refTags.wait();
            }
        }

        assertThat(refTags.get()).hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue));
    }

    @Test
    public void noContextProgapationViaConstructor() throws InterruptedException {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");

        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

        Thread thread;

        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            thread = new Thread(() -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                refTags.set(iter);

                synchronized (refTags) {
                    refTags.notifyAll();
                }
            });
        }

        thread.start();

        synchronized (refTags) {
            while (refTags.get() == null) {
                refTags.wait();
            }
        }

        assertThat(refTags.get()).hasSize(0);
    }
}
