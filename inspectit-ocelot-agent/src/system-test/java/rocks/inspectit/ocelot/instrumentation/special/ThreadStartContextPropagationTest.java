package rocks.inspectit.ocelot.instrumentation.special;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreadStartContextPropagationTest extends InstrumentationSysTestBase {

    private final static String attrKey = "test-key";

    private final static String attrValue = "test-value";

    /**
     * Abstract thread class.
     */
    private abstract class AbstractThread extends Thread {

        protected Runnable run;

        public AbstractThread(Runnable runnable) {
            run = runnable;
        }

        @Override
        public synchronized void start() {
            super.start();
        }

        @Override
        public void run() {
            run.run();
        }
    }

    private class SubThread extends AbstractThread {

        public SubThread(Runnable runnable) {
            super(runnable);
            setName("dummy-thread");
        }
    }

    @BeforeAll
    static void waitForClassInstrumentation() {
        TestUtils.waitForClassInstrumentations(Arrays.asList(AbstractThread.class, Thread.class), false, 15, TimeUnit.SECONDS);
    }

    @Test
    public void verifyContextPropagationViaAbstractThreads() throws InterruptedException {
        long rand = System.nanoTime();
        String attributeKey = attrKey + rand;
        String attributeValue = attrValue + rand;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Baggage> refBaggage = new AtomicReference<>();

        Thread thread = new SubThread(() -> {
            Baggage baggage = Baggage.current();
            refBaggage.set(baggage);
            latch.countDown();
        });

        try (Scope s = Baggage.current().toBuilder().put(attributeKey, attributeValue).build().makeCurrent()) {
            thread.start();
        }

        latch.await(5, TimeUnit.SECONDS);

        assertThat(refBaggage.get().asMap()).hasSize(1)
                .allSatisfy((key, valueEntry) -> {
                    assertThat(key).isEqualTo(attributeKey);
                    assertThat(valueEntry.getValue()).isEqualTo(attributeValue);
                });
    }

    @Test
    public void verifyContextPropagation() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Baggage> refBaggage = new AtomicReference<>();

        Thread thread = new SubThread(() -> {
            Baggage baggage = Baggage.current();
            refBaggage.set(baggage);
            latch.countDown();
        });

        try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
            thread.start();
        }

        latch.await(5, TimeUnit.SECONDS);

        assertThat(refBaggage.get().asMap()).hasSize(1)
                .allSatisfy((key, valueEntry) -> {
                    assertThat(key).isEqualTo(attrKey);
                    assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                });
    }

    @Test
    public void verifyContextPropagationUsingSubClasses() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Baggage> refBaggage = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            Baggage baggage = Baggage.current();
            refBaggage.set(baggage);
            latch.countDown();
        }) {
            @Override
            public synchronized void start() {
                super.start();
            }
        };

        try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
            thread.start();
        }

        latch.await(5, TimeUnit.SECONDS);

        assertThat(refBaggage.get().asMap()).hasSize(1)
                .allSatisfy((key, valueEntry) -> {
                    assertThat(key).isEqualTo(attrKey);
                    assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                });
    }

    @Test
    public void noContextPropagationViaConstructor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Baggage> refBaggage = new AtomicReference<>();

        Thread thread;

        try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
            thread = new Thread(() -> {
                Baggage baggage = Baggage.current();
                refBaggage.set(baggage);
                latch.countDown();
            });
        }

        thread.start();

        latch.await(5, TimeUnit.SECONDS);

        assertThat(refBaggage.get().asMap()).isEmpty();
    }

    @Test
    public void noCorrelationInExecutor() throws Exception {
        AtomicReference<Baggage> refBaggageInner = new AtomicReference<>();
        Runnable runnable = HelperClasses.getRunnableAsNamed(refBaggageInner);

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
            executorService.submit(runnable);
        }

        AtomicReference<Baggage> refBaggageOuter = new AtomicReference<>();
        Runnable runnableSecond = HelperClasses.getRunnableAsNamed(refBaggageOuter);
        Future<?> taskFuture = executorService.submit(runnableSecond); // have to be empty!
        taskFuture.get();

        assertThat(refBaggageInner.get().asMap()).hasSize(1)
                .allSatisfy((key, valueEntry) -> {
                    assertThat(key).isEqualTo(attrKey);
                    assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                });
        assertThat(refBaggageOuter.get().asMap()).isEmpty();
    }
}
