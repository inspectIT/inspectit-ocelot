package rocks.inspectit.oce.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BatchJobExecuterUnitTest {

    @Mock
    ScheduledExecutorService scheduledExecutor;

    @InjectMocks
    BatchJobExecutorService batchExecutor = new BatchJobExecutorService();


    @Test
    public void testJobTermination() {
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);

        when(scheduledExecutor.schedule(isA(Runnable.class), anyLong(), any())).then((invoc) -> {
            ((Runnable) invoc.getArgument(0)).run();
            return future;
        });

        Duration startDelay = Duration.ofMillis(10);
        Duration interBatchDelay = Duration.ofMillis(5);

        AtomicLong counter = new AtomicLong(0);
        batchExecutor.startJob((batchSize) -> {
            assertThat(batchSize).isEqualTo(42);
            if (counter.incrementAndGet() == 3) {
                return true;
            } else {
                return false;
            }
        }, 42, startDelay, interBatchDelay);

        verify(scheduledExecutor, times(1)).schedule(isA(Runnable.class), eq(startDelay.toNanos()), eq(TimeUnit.NANOSECONDS));
        verify(scheduledExecutor, times(2)).schedule(isA(Runnable.class), eq(interBatchDelay.toNanos()), eq(TimeUnit.NANOSECONDS));

    }


    @Test
    public void testJobBatchSizeUpdate() {
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);

        AtomicReference<Runnable> runnableToInvoke = new AtomicReference<>();

        when(scheduledExecutor.schedule(isA(Runnable.class), anyLong(), any())).then((invoc) -> {
            runnableToInvoke.set(((Runnable) invoc.getArgument(0)));
            return future;
        });

        Duration startDelay = Duration.ofMillis(10);
        Duration interBatchDelay = Duration.ofMillis(5);

        AtomicReference<BatchJobExecutorService.BatchJob<Integer>> job = new AtomicReference<>();

        job.set(batchExecutor.startJob((batchSize) -> {
            if (batchSize == 42) {
                job.get().setBatchSizes(10);
            } else if (batchSize == 10) {
                return true;
            }
            return false;
        }, 42, startDelay, interBatchDelay));
        verify(scheduledExecutor, times(1)).schedule(isA(Runnable.class), eq(startDelay.toNanos()), eq(TimeUnit.NANOSECONDS));
        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(1)).schedule(isA(Runnable.class), eq(interBatchDelay.toNanos()), eq(TimeUnit.NANOSECONDS));
        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(2)).schedule(isA(Runnable.class), anyLong(), any());

    }

    @Test
    public void testJobBatchDelayUpdate() {
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);

        AtomicReference<Runnable> runnableToInvoke = new AtomicReference<>();

        when(scheduledExecutor.schedule(isA(Runnable.class), anyLong(), any())).then((invoc) -> {
            runnableToInvoke.set(((Runnable) invoc.getArgument(0)));
            return future;
        });

        Duration startDelay = Duration.ofMillis(10);
        Duration interBatchDelay = Duration.ofMillis(5);

        AtomicReference<BatchJobExecutorService.BatchJob<Integer>> job = new AtomicReference<>();

        AtomicLong counter = new AtomicLong();
        job.set(batchExecutor.startJob((batchSize) -> {
            switch ((int) counter.incrementAndGet()) {
                case 1:
                    return false;
                case 2:
                    job.get().setInterBatchDelay(Duration.ofNanos(1234L));
                    return false;
                default:
                    return true;

            }
        }, 42, startDelay, interBatchDelay));
        verify(scheduledExecutor, times(1)).schedule(isA(Runnable.class), eq(startDelay.toNanos()), eq(TimeUnit.NANOSECONDS));
        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(1)).schedule(isA(Runnable.class), eq(interBatchDelay.toNanos()), eq(TimeUnit.NANOSECONDS));
        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(1)).schedule(isA(Runnable.class), eq(1234L), eq(TimeUnit.NANOSECONDS));
        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(3)).schedule(isA(Runnable.class), anyLong(), any());
    }


    @Test
    public void testExceptionsDontPreventFutureBatches() {
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);

        AtomicReference<Runnable> runnableToInvoke = new AtomicReference<>();

        when(scheduledExecutor.schedule(isA(Runnable.class), anyLong(), any())).then((invoc) -> {
            runnableToInvoke.set(((Runnable) invoc.getArgument(0)));
            return future;
        });

        Duration delay = Duration.ofMillis(10);

        AtomicLong counter = new AtomicLong();
        batchExecutor.startJob((batchSize) -> {
            switch ((int) counter.incrementAndGet()) {
                case 3:
                    return true;
                default:
                    throw new RuntimeException();

            }
        }, 42, delay, delay);

        verify(scheduledExecutor, times(1)).schedule(isA(Runnable.class), eq(delay.toNanos()), eq(TimeUnit.NANOSECONDS));
        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(2)).schedule(isA(Runnable.class), eq(delay.toNanos()), eq(TimeUnit.NANOSECONDS));
        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(3)).schedule(isA(Runnable.class), eq(delay.toNanos()), eq(TimeUnit.NANOSECONDS));
        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(3)).schedule(isA(Runnable.class), eq(delay.toNanos()), eq(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testJobCancellation() {
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);

        AtomicReference<Runnable> runnableToInvoke = new AtomicReference<>();

        when(scheduledExecutor.schedule(isA(Runnable.class), anyLong(), any())).then((invoc) -> {
            runnableToInvoke.set(((Runnable) invoc.getArgument(0)));
            return future;
        });

        Duration delay = Duration.ofMillis(10);

        BatchJobExecutorService.BatchJob<?> job = batchExecutor.startJob((batchSize) -> {
        }, 42, delay, delay);

        verify(scheduledExecutor, times(1)).schedule(isA(Runnable.class), eq(delay.toNanos()), eq(TimeUnit.NANOSECONDS));
        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(2)).schedule(isA(Runnable.class), eq(delay.toNanos()), eq(TimeUnit.NANOSECONDS));

        job.cancel();
        verify(future, times(1)).cancel(eq(false));

        runnableToInvoke.get().run();
        verify(scheduledExecutor, times(2)).schedule(isA(Runnable.class), eq(delay.toNanos()), eq(TimeUnit.NANOSECONDS));

    }

}
