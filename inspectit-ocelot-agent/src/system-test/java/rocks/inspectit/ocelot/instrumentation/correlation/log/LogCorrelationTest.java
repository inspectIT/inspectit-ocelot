package rocks.inspectit.ocelot.instrumentation.correlation.log;


import io.opencensus.trace.Tracing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class LogCorrelationTest {

    public static final String MDC_KEY = "traceid";

    @BeforeAll
    private static void beforeAll() throws InterruptedException {
        MDC.get("test"); //load the MDC classes
        TestUtils.waitForClassInstrumentation(LogCorrelationTest.class, 15, TimeUnit.SECONDS);
    }

    /**
     * This method gets instrumented with just tracing enabled based on the given sample probability.
     *
     * @param nested
     * @param sampleProbability
     */
    void traced(Runnable nested, double sampleProbability) {
        nested.run();
    }

    @Nested
    class ScopedSpan {

        @Test
        void verifyCorrelation() {
            assertThat(org.slf4j.MDC.get(MDC_KEY)).isNull();
            traced(() -> {
                String trace = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();
                assertThat(org.slf4j.MDC.get(MDC_KEY)).isEqualTo(trace);
            }, 1.0);
            assertThat(org.slf4j.MDC.get(MDC_KEY)).isNull();
        }

        @Test
        void verifyNoCorrelationForUnsampled() {
            assertThat(org.slf4j.MDC.get(MDC_KEY)).isNull();
            traced(() -> {
                assertThat(Tracing.getTracer().getCurrentSpan().getContext().isValid()).isTrue();
                assertThat(org.slf4j.MDC.get(MDC_KEY)).isNull();
            }, 0.0);
            assertThat(org.slf4j.MDC.get(MDC_KEY)).isNull();
        }
    }

    @Nested
    class NewThread {

        @Test
        void verifyCorrelation() throws InterruptedException {
            AtomicReference<Thread> newThread = new AtomicReference<>();
            traced(() -> {
                String trace = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();
                newThread.set(new Thread(() -> {
                    assertThat(MDC.get(MDC_KEY)).isEqualTo(trace);
                }));
            }, 1.0);

            newThread.get().start();
            newThread.get().join();
        }

        @Test
        void verifyNoCorrelationForUnsampled() throws InterruptedException {
            AtomicReference<Thread> newThread = new AtomicReference<>();
            traced(() -> {
                newThread.set(new Thread(() -> {
                    assertThat(Tracing.getTracer().getCurrentSpan().getContext().isValid()).isTrue();
                    assertThat(MDC.get(MDC_KEY)).isNull();
                }));
            }, 0.0);

            newThread.get().start();
            newThread.get().join();
        }
    }


    @Nested
    class ExecutorServiceCorrel {

        @Test
        void verifyRunnableCorrelation() throws InterruptedException, ExecutionException {
            ExecutorService es = Executors.newFixedThreadPool(1);
            AtomicReference<Future<?>> future = new AtomicReference<>();
            traced(() -> {
                String trace = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();
                future.set(es.submit(() -> {
                    assertThat(MDC.get(MDC_KEY)).isEqualTo(trace);
                }));
            }, 1.0);

            future.get().get();
        }

        @Test
        void verifyNoRunnableCorrelationForUnsampled() throws InterruptedException, ExecutionException {
            ExecutorService es = Executors.newFixedThreadPool(1);
            AtomicReference<Future<?>> future = new AtomicReference<>();
            traced(() -> {
                future.set(es.submit(() -> {
                    assertThat(Tracing.getTracer().getCurrentSpan().getContext().isValid()).isTrue();
                    assertThat(MDC.get(MDC_KEY)).isNull();
                }));
            }, 0.0);

            future.get().get();
        }


        @Test
        void verifyCallableCorrelation() throws InterruptedException, ExecutionException {
            ExecutorService es = Executors.newFixedThreadPool(1);
            AtomicReference<Future<?>> future = new AtomicReference<>();
            traced(() -> {
                String trace = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();
                future.set(es.submit(() -> {
                    assertThat(MDC.get(MDC_KEY)).isEqualTo(trace);
                    return "I'm a callable";
                }));
            }, 1.0);

            future.get().get();
        }

        @Test
        void verifyNoCallableCorrelationForUnsampled() throws InterruptedException, ExecutionException {
            ExecutorService es = Executors.newFixedThreadPool(1);
            AtomicReference<Future<?>> future = new AtomicReference<>();
            traced(() -> {
                future.set(es.submit(() -> {
                    assertThat(Tracing.getTracer().getCurrentSpan().getContext().isValid()).isTrue();
                    assertThat(MDC.get(MDC_KEY)).isNull();
                    return "I'm a callable";
                }));
            }, 0.0);

            future.get().get();
        }
    }


    @Nested
    class ScheduledExecutorService {

        @Test
        void verifyRunnableCorrelation() throws InterruptedException, ExecutionException {
            java.util.concurrent.ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
            AtomicReference<Future<?>> future = new AtomicReference<>();
            traced(() -> {
                String trace = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();
                future.set(es.schedule(() -> {
                    assertThat(MDC.get(MDC_KEY)).isEqualTo(trace);
                }, 10, TimeUnit.MILLISECONDS));
            }, 1.0);
            future.get().get();
            es.shutdown();
        }

        @Test
        void verifyNoRunnableCorrelationForUnsampled() throws InterruptedException, ExecutionException {
            java.util.concurrent.ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
            AtomicReference<Future<?>> future = new AtomicReference<>();
            traced(() -> {
                future.set(es.schedule(() -> {
                    assertThat(Tracing.getTracer().getCurrentSpan().getContext().isValid()).isTrue();
                    assertThat(MDC.get(MDC_KEY)).isNull();
                }, 10, TimeUnit.MILLISECONDS));
            }, 0.0);

            future.get().get();
        }


        @Test
        void verifyCallableCorrelation() throws InterruptedException, ExecutionException {
            java.util.concurrent.ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
            AtomicReference<Future<?>> future = new AtomicReference<>();
            traced(() -> {
                String trace = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();
                future.set(es.schedule(() -> {
                    assertThat(MDC.get(MDC_KEY)).isEqualTo(trace);
                    return "I'm a callable";
                }, 10, TimeUnit.MILLISECONDS));
            }, 1.0);
            future.get().get();
            es.shutdown();
        }

        @Test
        void verifyNoCallableCorrelationForUnsampled() throws InterruptedException, ExecutionException {
            java.util.concurrent.ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
            AtomicReference<Future<?>> future = new AtomicReference<>();
            traced(() -> {
                future.set(es.schedule(() -> {
                    assertThat(Tracing.getTracer().getCurrentSpan().getContext().isValid()).isTrue();
                    assertThat(MDC.get(MDC_KEY)).isNull();
                    return "I'm a callable";
                }, 10, TimeUnit.MILLISECONDS));
            }, 0.0);

            future.get().get();
        }

        @Test
        void verifyScheduleWithFixedDelayCorrelation() throws InterruptedException, ExecutionException {
            java.util.concurrent.ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
            CountDownLatch csl = new CountDownLatch(3);
            traced(() -> {
                String trace = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();
                es.scheduleWithFixedDelay(() -> {
                    assertThat(MDC.get(MDC_KEY)).isEqualTo(trace);
                    csl.countDown();
                }, 0, 5, TimeUnit.MILLISECONDS);
            }, 1.0);
            csl.await();
            es.shutdown();
        }

        @Test
        void verifyNoScheduleWithFixedDelayCorrelationForUnsampled() throws InterruptedException, ExecutionException {
            java.util.concurrent.ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
            CountDownLatch csl = new CountDownLatch(3);
            traced(() -> {
                es.scheduleWithFixedDelay(() -> {
                    assertThat(Tracing.getTracer().getCurrentSpan().getContext().isValid()).isTrue();
                    assertThat(MDC.get(MDC_KEY)).isNull();
                    csl.countDown();
                }, 0, 5, TimeUnit.MILLISECONDS);
            }, 0.0);
            csl.await();
            es.shutdown();
        }
    }


}
