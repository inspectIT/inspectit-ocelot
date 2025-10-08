package rocks.inspectit.ocelot.instrumentation.special;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
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

    private ScheduledExecutorService executorService;

    private static final String attrKey = "test_key";

    private static final String attrValue = "test_value";

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
            AtomicReference<Baggage> refBaggage = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsLambda(refBaggage);

            ScheduledFuture<?> schedule;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
            }
            schedule.get();

            assertThat(refBaggage.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void verifyCtxPropagationViaScheduleRunnable_anonymous() throws Exception {
            AtomicReference<Baggage> refBaggage = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsAnonymous(refBaggage);
            TestUtils.waitForClassInstrumentations(runnable.getClass());

            ScheduledFuture<?> schedule;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
            }
            schedule.get();

            assertThat(refBaggage.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void verifyCtxPropagationViaScheduleRunnable_named() throws Exception {
            AtomicReference<Baggage> refBaggage = new AtomicReference<>();

            Runnable runnable = HelperClasses.getRunnableAsNamed(refBaggage);

            ScheduledFuture<?> schedule;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
            }
            schedule.get();

            assertThat(refBaggage.get().asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }
    }

    @Nested
    public class Schedule_callable {

        @Test
        public void verifyCtxPropagationViaScheduleCallable_lambda() throws Exception {
            Callable<Baggage> callable = HelperClasses.getCallableAsLambda();

            ScheduledFuture<Baggage> future;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
            }
            Baggage result = future.get();

            assertThat(result.asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void verifyCtxPropagationViaScheduleCallable_anonymous() throws Exception {
            Callable<Baggage> callable = HelperClasses.getCallableAsAnonymous();
            TestUtils.waitForClassInstrumentations(callable.getClass());

            ScheduledFuture<Baggage> future;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
            }
            Baggage result = future.get();

            assertThat(result.asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }

        @Test
        public void verifyCtxPropagationViaScheduleCallable_named() throws Exception {
            Callable<Baggage> callable = HelperClasses.getCallableAsNamed();

            ScheduledFuture<Baggage> future;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
            }
            Baggage result = future.get();

            assertThat(result.asMap()).hasSize(1)
                    .allSatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo(attrKey);
                        assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                    });
        }
    }

    @Nested
    public class ScheduleWithFixedDelay {

        @Test
        public void verifyCtxPropagationViaScheduleFixedDelay_lambda() throws Exception {
            int iterations = 5;
            CountDownLatch iterationCount = new CountDownLatch(iterations);

            List<Baggage> baggageList = new CopyOnWriteArrayList<>();

            Runnable runnable = () -> {
                Baggage baggage = Baggage.current();
                baggageList.add(baggage);
                iterationCount.countDown();
            };

            ScheduledFuture future;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                future = executorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            iterationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(baggageList).size().isGreaterThanOrEqualTo(iterations);
            baggageList.forEach(baggage ->
                    assertThat(baggage.asMap()).hasSize(1)
                            .allSatisfy((key, valueEntry) -> {
                                assertThat(key).isEqualTo(attrKey);
                                assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                            })
            );
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedDelay_anonymous() throws Exception {
            int iterations = 5;
            CountDownLatch iterationCount = new CountDownLatch(iterations);

            List<Baggage> baggageList = new CopyOnWriteArrayList<>();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Baggage baggage = Baggage.current();
                    baggageList.add(baggage);
                    iterationCount.countDown();
                }
            };
            TestUtils.waitForClassInstrumentations(runnable.getClass());

            ScheduledFuture future;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                future = executorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            iterationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(baggageList).size().isGreaterThanOrEqualTo(iterations);
            baggageList.forEach(baggage ->
                    assertThat(baggage.asMap()).hasSize(1)
                            .allSatisfy((key, valueEntry) -> {
                                assertThat(key).isEqualTo(attrKey);
                                assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                            })
            );
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedDelay_named() throws Exception {
            int iterations = 5;
            CountDownLatch iterationCount = new CountDownLatch(iterations);

            List<Baggage> baggageList = new CopyOnWriteArrayList<>();

            Runnable runnable = new TestRunnable(unused -> {
                Baggage baggage = Baggage.current();
                baggageList.add(baggage);
                iterationCount.countDown();
            });
            TestUtils.waitForClassInstrumentations(runnable.getClass());

            ScheduledFuture future;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                future = executorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            iterationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(baggageList).size().isGreaterThanOrEqualTo(iterations);
            baggageList.forEach(baggage ->
                    assertThat(baggage.asMap()).hasSize(1)
                            .allSatisfy((key, valueEntry) -> {
                                assertThat(key).isEqualTo(attrKey);
                                assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                            })
            );
        }
    }

    @Nested
    public class ScheduleAtFixedRate {

        @Test
        public void verifyCtxPropagationViaScheduleFixedRate_lambda() throws Exception {
            int iterations = 5;
            CountDownLatch iterationCount = new CountDownLatch(iterations);

            List<Baggage> baggageList = new CopyOnWriteArrayList<>();

            Runnable runnable = () -> {
                Baggage baggage = Baggage.current();
                baggageList.add(baggage);
                iterationCount.countDown();
            };

            ScheduledFuture future;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                future = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            iterationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(baggageList).size().isGreaterThanOrEqualTo(iterations);
            baggageList.forEach(baggage ->
                    assertThat(baggage.asMap()).hasSize(1)
                            .allSatisfy((key, valueEntry) -> {
                                assertThat(key).isEqualTo(attrKey);
                                assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                            })
            );
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedRate_anonymous() throws Exception {
            int iterations = 5;
            CountDownLatch iterationCount = new CountDownLatch(iterations);

            List<Baggage> baggageList = new CopyOnWriteArrayList<>();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Baggage baggage = Baggage.current();
                    baggageList.add(baggage);
                    iterationCount.countDown();
                }
            };
            TestUtils.waitForClassInstrumentations(runnable.getClass()); // wait for anonymous class instrumentation

            ScheduledFuture future;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                future = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            iterationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(baggageList).size().isGreaterThanOrEqualTo(iterations);
            baggageList.forEach(baggage ->
                    assertThat(baggage.asMap()).hasSize(1)
                            .allSatisfy((key, valueEntry) -> {
                                assertThat(key).isEqualTo(attrKey);
                                assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                            })
            );
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedRate_named() throws Exception {
            int iterations = 5;
            CountDownLatch iterationCount = new CountDownLatch(iterations);

            List<Baggage> baggageList = new CopyOnWriteArrayList<>();

            Runnable runnable = new TestRunnable(unused -> {
                Baggage baggage = Baggage.current();
                baggageList.add(baggage);
                iterationCount.countDown();
            });

            ScheduledFuture future;
            try (Scope s = Baggage.current().toBuilder().put(attrKey, attrValue).build().makeCurrent()) {
                future = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            iterationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(baggageList).size().isGreaterThanOrEqualTo(iterations);
            baggageList.forEach(baggage ->
                    assertThat(baggage.asMap()).hasSize(1)
                            .allSatisfy((key, valueEntry) -> {
                                assertThat(key).isEqualTo(attrKey);
                                assertThat(valueEntry.getValue()).isEqualTo(attrValue);
                            })
            );
        }
    }
}
