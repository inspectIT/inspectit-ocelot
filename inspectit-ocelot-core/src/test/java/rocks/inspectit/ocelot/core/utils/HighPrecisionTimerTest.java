package rocks.inspectit.ocelot.core.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.awaitility.Awaitility.await;
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

    @Nested
    class Start {

        @Test
        void verifyStartResetsInactivity() {
            createSpyTimer(1, 100, () -> false);
            for (int i = 0; i < 10; i++) {
                timer.start();
                await().atMost(5, TimeUnit.SECONDS).until(timer::isStarted);
            }
            verify(timer).startTimerSynchronized();
        }


        @Test
        void verifyActionCalled() {
            BooleanSupplier action = mock(BooleanSupplier.class);
            doReturn(true).when(action).getAsBoolean();

            createSpyTimer(1, 10, action);

            timer.start();
            await().atMost(5, TimeUnit.SECONDS).until(timer::isStarted);

            verify(action, atLeastOnce()).getAsBoolean();
        }

        @Test
        void verifyActionResetsInactivity() {
            createSpyTimer(1, 10, () -> true);

            timer.start();
            await().atMost(5, TimeUnit.SECONDS).until(timer::isStarted);
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

            await().atMost(5, TimeUnit.SECONDS).until(timer::isStarted);
            for (int i = 0; i < 2; i++) {
                timer.start();
            }
            verify(timer, times(2)).startTimerSynchronized();
        }
    }
}