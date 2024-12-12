package rocks.inspectit.ocelot.core.config.propertysources.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TaskTimeoutExecutorTest {

    private TaskTimeoutExecutor timeoutExecutor;

    private Future<?> task;

    @Mock
    private Runnable restartTask;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final Duration timeout = Duration.ofMillis(1000);

    @BeforeEach
    void beforeEach() {
        timeoutExecutor = new TaskTimeoutExecutor();
    }

    @AfterEach
    void afterEach() {
        task.cancel(true);
    }

    @Test
    void shouldCancelTaskAndRestartWhenTimeoutExceeded() throws InterruptedException {
        // Arrange - Prepare task
        Runnable endlessLoop = () -> {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        task = executor.schedule(endlessLoop, 500, TimeUnit.MILLISECONDS);

        // Act - Setup timeout executor for cancelling
        timeoutExecutor.scheduleCancelling(task, "test", restartTask, timeout);
        // Wait for timeout to elapse
        Thread.sleep(timeout.toMillis() + 100);

        // Assert
        assertTrue(task.isCancelled());
        verify(restartTask, times(1)).run();
    }

    @Test
    void shouldNotCancelTaskWhenNoTimeout() throws InterruptedException {
        // Arrange - Prepare task
        Runnable worker = () -> {
            System.out.println("Work done");
            // Act 2 - Cancel timeout after work is done
            timeoutExecutor.cancelTimeout();
        };
        task = executor.schedule(worker, 500, TimeUnit.MILLISECONDS);

        // Act 1 - Setup timeout executor for cancelling
        timeoutExecutor.scheduleCancelling(task, "test", restartTask, timeout);
        // Wait for timeout to elapse
        Thread.sleep(timeout.toMillis() + 100);

        // Assert
        assertFalse(task.isCancelled());
        verifyNoInteractions(restartTask);
    }
}
