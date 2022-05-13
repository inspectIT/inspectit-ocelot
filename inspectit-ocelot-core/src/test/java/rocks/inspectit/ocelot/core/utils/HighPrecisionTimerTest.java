package rocks.inspectit.ocelot.core.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HighPrecisionTimerTest {

    private HighPrecisionTimer timer;

    private void createSpyTimer(long periodMillis, long inactivityLimitMillis, BooleanSupplier function) {
        timer = Mockito.spy(new HighPrecisionTimer("test", Duration.ofMillis(periodMillis), Duration.ofMillis(inactivityLimitMillis), function));
    }

    @AfterEach
    void cleanUp() {
        timer.destroy();
    }

    private void sleepAtLeast(long ms) {
        long start = System.nanoTime();
        while ((System.nanoTime() - start) < ms * 1000 * 1000) {
            long slept = (System.nanoTime() - start) / 1000 / 1000;
            try {
                Thread.sleep(ms - slept);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    class Start {

        @Test
        void verifyStartResetsInactivity() {
            createSpyTimer(1, 100, () -> false);
            for (int i = 0; i < 10; i++) {
                timer.start();
                sleepAtLeast(20);
            }
            verify(timer).startTimerSynchronized();
        }


        @Test
        void verifyActionCalled() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            BooleanSupplier action = () -> {
                latch.countDown();
                return true;
            };

            createSpyTimer(1, 10, action);

            timer.start();

            boolean reachedZero = latch.await(5, TimeUnit.SECONDS);
            assertThat(reachedZero).isTrue();
        }

        @Test
        void verifyActionResetsInactivity() {
            createSpyTimer(1, 10, () -> true);

            timer.start();
            sleepAtLeast(50);
            timer.start();

            verify(timer).startTimerSynchronized();
        }

        @Test
        void verifyRestartAfterInactivity() {
            createSpyTimer(1, 100, () -> false);
            for (int i = 0; i < 2; i++) {
                timer.start();
            }
            verify(timer).startTimerSynchronized();

            sleepAtLeast(150);
            for (int i = 0; i < 2; i++) {
                timer.start();
            }
            verify(timer, times(2)).startTimerSynchronized();
        }
    }
}