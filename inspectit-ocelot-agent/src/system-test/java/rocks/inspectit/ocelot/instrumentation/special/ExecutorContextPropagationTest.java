package rocks.inspectit.ocelot.instrumentation.special;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.*;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.instrumentation.special.HelperClasses.TestCallable;
import rocks.inspectit.ocelot.instrumentation.special.HelperClasses.TestRunnable;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ExecutorContextPropagationTest extends InstrumentationSysTestBase {

    private ExecutorService executorService;

    private static final String attrKey = "test_key";

    private static final String attrValue = "test_value";

    @BeforeAll
    public static void beforeAll() {
        TestUtils.waitForClassInstrumentations(ThreadPoolExecutor.class, TestRunnable.class, TestCallable.class);
    }

    @BeforeEach
    public void beforeEach() throws ExecutionException, InterruptedException {
        // warmup the executor - if this is not be done, the first call to the executor will always be correlated
        // because a thread is started, thus, it is correlated due to the Thread.start correlation
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(Math::random).get();
    }

    @AfterEach
    public void afterEach() {
        executorService.shutdown();
    }

    @Nested
    public class Submit_runnable {

        @Test
        public void correlateRunnable_lambda() throws Exception {
            AtomicReference<Baggage> refBaggage = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsLambda(refBaggage);

            Future<?> taskFuture;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                taskFuture = executorService.submit(runnable);
            }

            taskFuture.get();

            assertThat(refBaggage.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void correlateRunnable_anonymous() throws Exception {
            AtomicReference<Baggage> refBaggage = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsAnonymous(refBaggage);
            TestUtils.waitForClassInstrumentations(runnable.getClass());

            Future<?> taskFuture;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                taskFuture = executorService.submit(runnable);
            }
            taskFuture.get();

            assertThat(refBaggage.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void correlateRunnable_named() throws Exception {
            AtomicReference<Baggage> refBaggage = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsNamed(refBaggage);

            Future<?> taskFuture;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                taskFuture = executorService.submit(runnable);
            }
            taskFuture.get();

            assertThat(refBaggage.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }
    }

    @Nested
    public class Execute_runnable {

        @Test
        public void correlateRunnable_lambda() throws Exception {
            AtomicReference<Baggage> refBaggage = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Runnable runnable = () -> {
                refBaggage.set(Baggage.current());
                latch.countDown();
            };

            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                executorService.execute(runnable);
            }

            latch.await();

            assertThat(refBaggage.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void correlateRunnable_anonymous() throws Exception {
            AtomicReference<Baggage> refBaggage = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    refBaggage.set(Baggage.current());
                    latch.countDown();
                }
            };
            TestUtils.waitForClassInstrumentations(runnable.getClass());

            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                executorService.execute(runnable);
            }

            latch.await();

            assertThat(refBaggage.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void correlateRunnable_named() throws Exception {
            AtomicReference<Baggage> refBaggage = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Runnable runnable = new TestRunnable(unused -> {
                refBaggage.set(Baggage.current());
                latch.countDown();
            });

            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                executorService.execute(runnable);
            }

            latch.await();

            assertThat(refBaggage.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }
    }

    @Nested
    public class Submit_callable {

        @Test
        public void submitCallable_lambda() throws Exception {
            Callable<Baggage> callable = HelperClasses.getCallableAsLambda();

            Future<Baggage> result;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                result = executorService.submit(callable);
            }

            assertThat(result.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void submitCallable_anonymous() throws Exception {
            Callable<Baggage> callable = HelperClasses.getCallableAsAnonymous();
            TestUtils.waitForClassInstrumentations(callable.getClass());

            Future<Baggage> result;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                result = executorService.submit(callable);
            }

            assertThat(result.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void submitCallable_named() throws Exception {
            Callable<Baggage> callable = HelperClasses.getCallableAsNamed();

            Future<Baggage> result;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                result = executorService.submit(callable);
            }

            assertThat(result.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }
    }
}
