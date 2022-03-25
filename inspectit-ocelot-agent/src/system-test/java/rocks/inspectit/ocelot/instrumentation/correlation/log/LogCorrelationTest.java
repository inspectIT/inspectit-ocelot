package rocks.inspectit.ocelot.instrumentation.correlation.log;

import io.opencensus.trace.Tracing;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import rocks.inspectit.ocelot.instrumentation.special.HelperClasses.TestCallable;
import rocks.inspectit.ocelot.instrumentation.special.HelperClasses.TestRunnable;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class LogCorrelationTest {

    public static final String MDC_KEY = "traceid";

    private ExecutorService executorService;

    private ScheduledExecutorService scheduledExecutorService;

    private static Function<String, String> getTestMdc;

    /**
     * Simulates the case where a class loader is isolated (e.g. in JBoss module system) and does not have access
     * to the ocelot bootstrap classes.
     */
    static class IsolatedMdcClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("rocks.inspectit.ocelot.bootstrap")) {
                throw new ClassNotFoundException();
            }

            // only this class should be loaded, otherwise we delegate the loading to the parent
            if (!name.startsWith("org.slf4j.MDC")) {
                return super.loadClass(name);
            }

            System.out.println("Load " + name);

            try {
                String replace = name.replace(".", "/");
                InputStream in = ClassLoader.getSystemResourceAsStream(replace + ".class");
                byte[] a = new byte[10000];
                int len = in.read(a);
                in.close();
                return defineClass(name, a, 0, len);
            } catch (IOException e) {
                throw new ClassNotFoundException();
            }
        }
    }

    @BeforeAll
    private static void beforeAll() throws Exception {
        Class<?> testMdcClass = new IsolatedMdcClassLoader().loadClass("org.slf4j.MDC");
        final Method getMethod = testMdcClass.getMethod("get", String.class);

        getTestMdc = (key) -> {
            try {
                return (String) getMethod.invoke(null, key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };


        //load the MDC classes
        MDC.get("test");
        org.apache.log4j.MDC.get("test");
        ThreadContext.get("test");

        TestUtils.waitForClassInstrumentations(
                Thread.class,
                AbstractExecutorService.class,
                ThreadPoolExecutor.class,
                ScheduledThreadPoolExecutor.class,
                LogCorrelationTest.class,
                TestRunnable.class,
                TestCallable.class);
        TestUtils.waitForClassHooks(LogCorrelationTest.class);
    }

    @BeforeEach
    public void beforeEach() throws ExecutionException, InterruptedException {
        // warmup the executor - if this is not be done, the first call to the executor will always be correlated
        // because a thread is started, thus, it is correlated due to the Thread.start correlation
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(Math::random).get();

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.submit(Math::random).get();
    }

    @AfterEach
    public void afterEach() {
        executorService.shutdown();
        scheduledExecutorService.shutdown();
    }

    /**
     * This method gets instrumented with just tracing enabled based on the given sample probability.
     * The 'sampleProbability' parameter is used by rules to determine whether a trace should be created or not.
     */
    void traced(Runnable nested, double sampleProbability) {
        nested.run();
    }

    void assertMDCContainTraceId(String expected) {
        if (expected == null) {
            assertThat(org.slf4j.MDC.get(MDC_KEY)).isNull();
            assertThat(org.apache.logging.log4j.ThreadContext.get(MDC_KEY)).isNull();
            assertThat(org.apache.log4j.MDC.get(MDC_KEY)).isNull();
            assertThat(getTestMdc.apply(MDC_KEY)).isNull();
        } else {
            assertThat(org.slf4j.MDC.get(MDC_KEY)).isEqualTo(expected);
            assertThat(org.apache.logging.log4j.ThreadContext.get(MDC_KEY)).isEqualTo(expected);
            assertThat(org.apache.log4j.MDC.get(MDC_KEY)).isEqualTo(expected);
            assertThat(getTestMdc.apply(MDC_KEY)).isEqualTo(expected);
        }
    }

    private boolean isMdcTraceIdEqualTo(String expected) {
        String slf4j = org.slf4j.MDC.get(MDC_KEY);
        String log4j = (String) org.apache.log4j.MDC.get(MDC_KEY);
        String log4j2 = org.apache.logging.log4j.ThreadContext.get(MDC_KEY);

        if (expected == null) {
            return slf4j == null && log4j == null && log4j2 == null;
        } else {
            return expected.equals(slf4j) && expected.equals(log4j) && expected.equals(log4j2);
        }
    }

    private String currentTraceId() {
        return Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();
    }

    @Nested
    class ScopedSpan {

        @Test
        void verifyCorrelation() {
            assertMDCContainTraceId(null);
            traced(() -> {
                String currentTraceId = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();
                assertMDCContainTraceId(currentTraceId);
            }, 1.0);
            assertMDCContainTraceId(null);
        }

        @Test
        void verifyNoCorrelationForUnsampled() {
            assertMDCContainTraceId(null);
            traced(() -> {
                assertThat(Tracing.getTracer().getCurrentSpan().getContext().isValid()).isTrue();
                assertMDCContainTraceId(null);
            }, 0.0);
            assertMDCContainTraceId(null);
        }
    }

    @Nested
    class Thread_start {

        @Test
        void verifyCorrelation_lambda() throws InterruptedException {
            AtomicReference<Thread> newThread = new AtomicReference<>();
            MutableBoolean isExpectedTraceId = new MutableBoolean();

            traced(() -> {
                String trace = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();

                Thread thread = new Thread(() -> {
                    isExpectedTraceId.setValue(isMdcTraceIdEqualTo(trace));
                });

                // do correlation
                thread.start();

                newThread.set(thread);
            }, 1.0);

            newThread.get().join();

            assertThat(isExpectedTraceId.booleanValue()).isTrue();
        }

        @Test
        void verifyCorrelation_anonymousThread() throws InterruptedException {
            AtomicReference<Thread> newThread = new AtomicReference<>();
            MutableBoolean isExpectedTraceId = new MutableBoolean();

            traced(() -> {
                String trace = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        isExpectedTraceId.setValue(isMdcTraceIdEqualTo(trace));
                    }
                };

                // do correlation
                thread.start();

                newThread.set(thread);
            }, 1.0);

            newThread.get().join();

            assertThat(isExpectedTraceId.booleanValue()).isTrue();
        }

        @Test
        void verifyNoCorrelationForUnsampled() throws InterruptedException {
            AtomicReference<Thread> newThread = new AtomicReference<>();
            MutableBoolean isValidTrace = new MutableBoolean();
            MutableBoolean isExpectedTraceId = new MutableBoolean();

            traced(() -> {
                newThread.set(new Thread(() -> {
                    isValidTrace.setValue(Tracing.getTracer().getCurrentSpan().getContext().isValid());
                    isExpectedTraceId.setValue(isMdcTraceIdEqualTo(null));
                }));
            }, 0.0);

            newThread.get().start();
            newThread.get().join();

            assertThat(isValidTrace.booleanValue()).isFalse(); // no trace exists
            assertThat(isExpectedTraceId.booleanValue()).isTrue();
        }
    }

    @Nested
    class ExecutorService_execute {

        @Test
        void verifyRunnableCorrelation_lambda() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            MutableBoolean isExpectedTraceId = new MutableBoolean();

            traced(() -> {
                String traceId = currentTraceId();

                Runnable runnable = () -> {
                    isExpectedTraceId.setValue(isMdcTraceIdEqualTo(traceId));
                    latch.countDown();
                };

                executorService.execute(runnable);
            }, 1.0);

            latch.await();

            assertThat(isExpectedTraceId.booleanValue()).isTrue();
        }

        @Test
        void verifyRunnableCorrelation_named() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            MutableBoolean isExpectedTraceId = new MutableBoolean();

            traced(() -> {
                String traceId = currentTraceId();

                Runnable runnable = new TestRunnable(unused -> {
                    isExpectedTraceId.setValue(isMdcTraceIdEqualTo(traceId));
                    latch.countDown();
                });

                executorService.execute(runnable);
            }, 1.0);

            latch.await();

            assertThat(isExpectedTraceId.booleanValue()).isTrue();
        }

    }

    @Nested
    class ExecutorService_submit {

        @Test
        void verifyNoRunnableCorrelationForUnsampled() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            MutableBoolean isExpectedTraceId = new MutableBoolean();

            traced(() -> {
                executorService.submit(() -> {
                    isExpectedTraceId.setValue(isMdcTraceIdEqualTo(null));
                    latch.countDown();
                });
            }, 0.0);

            latch.await();

            assertThat(isExpectedTraceId.booleanValue()).isTrue();
        }

        @Test
        void verifyCallableCorrelation_lambda() throws Exception {
            AtomicReference<Future<Boolean>> futureResult = new AtomicReference<>();

            traced(() -> {
                String currentTraceId = currentTraceId();

                Callable<Boolean> callable = () -> isMdcTraceIdEqualTo(currentTraceId);

                Future<Boolean> result = executorService.submit(callable);
                futureResult.set(result);

            }, 1.0);

            boolean isExpectedTraceId = futureResult.get().get();
            assertThat(isExpectedTraceId).isTrue();
        }

        @Test
        void verifyCallableCorrelation_named() throws Exception {
            AtomicReference<Future<Boolean>> futureResult = new AtomicReference<>();

            traced(() -> {
                String currentTraceId = currentTraceId();

                Callable<Boolean> callable = new TestCallable<>(unused -> isMdcTraceIdEqualTo(currentTraceId));

                Future<Boolean> result = executorService.submit(callable);
                futureResult.set(result);

            }, 1.0);

            boolean isExpectedTraceId = futureResult.get().get();
            assertThat(isExpectedTraceId).isTrue();
        }

        @Test
        void verifyNoCallableCorrelationForUnsampled() throws Exception {
            AtomicReference<Future<Boolean>> futureResult = new AtomicReference<>();

            traced(() -> {
                Callable<Boolean> callable = () -> isMdcTraceIdEqualTo(null);

                Future<Boolean> result = executorService.submit(callable);
                futureResult.set(result);

            }, 0.0);

            boolean isExpectedTraceId = futureResult.get().get();
            assertThat(isExpectedTraceId).isTrue();
        }
    }

    @Nested
    class ScheduledExecutorService_schedule {

        @Test
        void verifyRunnableCorrelation_lambda() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            MutableBoolean isExpectedTraceId = new MutableBoolean();

            traced(() -> {
                String currentTraceId = currentTraceId();

                Runnable runnable = () -> {
                    isExpectedTraceId.setValue(isMdcTraceIdEqualTo(currentTraceId));
                    latch.countDown();
                };

                scheduledExecutorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);

            }, 1.0);

            latch.await();

            assertThat(isExpectedTraceId.booleanValue()).isTrue();
        }

        @Test
        void verifyRunnableCorrelation_named() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            MutableBoolean isExpectedTraceId = new MutableBoolean();

            traced(() -> {
                String currentTraceId = currentTraceId();

                Runnable runnable = new TestRunnable(unused -> {
                    isExpectedTraceId.setValue(isMdcTraceIdEqualTo(currentTraceId));
                    latch.countDown();
                });

                scheduledExecutorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);

            }, 1.0);

            latch.await();

            assertThat(isExpectedTraceId.booleanValue()).isTrue();
        }

        @Test
        void verifyNoRunnableCorrelationForUnsampled() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            MutableBoolean isExpectedTraceId = new MutableBoolean();

            traced(() -> {
                Runnable runnable = () -> {
                    isExpectedTraceId.setValue(isMdcTraceIdEqualTo(null));
                    latch.countDown();
                };

                scheduledExecutorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);

            }, 0.0);

            latch.await();

            assertThat(isExpectedTraceId.booleanValue()).isTrue();
        }

        @Test
        void verifyCallableCorrelation_lambda() throws Exception {
            AtomicReference<ScheduledFuture<Boolean>> resultFuture = new AtomicReference<>();

            traced(() -> {
                String currentTraceId = currentTraceId();

                Callable<Boolean> callable = () -> isMdcTraceIdEqualTo(currentTraceId);

                ScheduledFuture<Boolean> result = scheduledExecutorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
                resultFuture.set(result);
            }, 1.0);

            Boolean isExpectedTraceId = resultFuture.get().get();
            assertThat(isExpectedTraceId).isTrue();
        }

        @Test
        void verifyCallableCorrelation_named() throws Exception {
            AtomicReference<ScheduledFuture<Boolean>> resultFuture = new AtomicReference<>();

            traced(() -> {
                String currentTraceId = currentTraceId();

                TestCallable<Boolean> callable = new TestCallable<>(unused -> isMdcTraceIdEqualTo(currentTraceId));

                ScheduledFuture<Boolean> result = scheduledExecutorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
                resultFuture.set(result);
            }, 1.0);

            Boolean isExpectedTraceId = resultFuture.get().get();
            assertThat(isExpectedTraceId).isTrue();
        }

        @Test
        void verifyNoCallableCorrelationForUnsampled() throws Exception {
            AtomicReference<ScheduledFuture<Boolean>> resultFuture = new AtomicReference<>();

            traced(() -> {
                Callable<Boolean> callable = () -> isMdcTraceIdEqualTo(null);

                ScheduledFuture<Boolean> result = scheduledExecutorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
                resultFuture.set(result);
            }, 0.0);

            Boolean isExpectedTraceId = resultFuture.get().get();
            assertThat(isExpectedTraceId).isTrue();
        }
    }

    @Nested
    class ScheduledExecutorService_scheduleWithFixedDelay {

        @Test
        void verifyScheduleWithFixedDelayCorrelation_lambda() throws Exception {
            CountDownLatch latch = new CountDownLatch(10);
            Deque<Boolean> deque = new LinkedBlockingDeque<>();

            traced(() -> {
                String currentTraceId = currentTraceId();

                Runnable runnable = () -> {
                    deque.add(isMdcTraceIdEqualTo(currentTraceId));
                    latch.countDown();
                };

                scheduledExecutorService.scheduleWithFixedDelay(runnable, 1, 10, TimeUnit.MILLISECONDS);

            }, 1.0);

            latch.await();

            assertThat(deque).size().isGreaterThanOrEqualTo(10);
            // remove all elements until the first "true" one
            // this is done in case the instrumentation is a bit delayed
            while (deque.peek() != null && !deque.peek()) {
                deque.pop();
            }
            assertThat(deque).extracting(Boolean::booleanValue)
                    .containsOnly(true);
        }

        @Test
        void verifyScheduleWithFixedDelayCorrelation_named() throws Exception {
            CountDownLatch latch = new CountDownLatch(5);
            Deque<Boolean> deque = new LinkedBlockingDeque<>();

            traced(() -> {
                String currentTraceId = currentTraceId();

                Runnable runnable = new TestRunnable(unused -> {
                    deque.add(isMdcTraceIdEqualTo(currentTraceId));
                    latch.countDown();
                });

                scheduledExecutorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);

            }, 1.0);

            latch.await();

            assertThat(deque).size().isGreaterThanOrEqualTo(5);
            assertThat(deque).extracting(Boolean::booleanValue)
                    .containsOnly(true);
        }

        @Test
        void verifyNoScheduleWithFixedDelayCorrelationForUnsampled() throws InterruptedException, ExecutionException {
            CountDownLatch latch = new CountDownLatch(5);
            Deque<Boolean> deque = new LinkedBlockingDeque<>();

            traced(() -> {
                Runnable runnable = () -> {
                    deque.add(isMdcTraceIdEqualTo(null));
                    latch.countDown();
                };

                scheduledExecutorService.scheduleWithFixedDelay(runnable, 0, 10, TimeUnit.MILLISECONDS);

            }, 0.0);

            latch.await();

            assertThat(deque).size().isGreaterThanOrEqualTo(5);
            assertThat(deque).extracting(Boolean::booleanValue)
                    .containsOnly(true);
        }
    }

}
