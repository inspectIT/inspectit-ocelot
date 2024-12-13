package rocks.inspectit.ocelot.core.config.propertysources.http;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * Cancels internal tasks after a timeout has been exceeded.
 */
@Slf4j
public class TaskTimeoutExecutor {

    /**
     * The future to cancel the task by timeout.
     */
    private Future<?> timeoutExecutor;

    /**
     * Cancel the task after a specific timeout. This should prevent, that the thread stays deadlocked.
     * The task will be restarted after successful cancel.
     *
     * @param task the task to cancel by timeout
     * @param taskName the name of the task
     * @param restartTask the runnable to restart the task
     * @param timeout the time after which the task should be cancelled
     */
    public void scheduleCancelling(Future<?> task, String taskName, Runnable restartTask, Duration timeout) {
        ThreadFactory factory = new ThreadFactoryBuilder()
                .setNameFormat("timeout-" + taskName)
                .build();
        ScheduledExecutorService cancelExecutor = Executors.newSingleThreadScheduledExecutor(factory);

        // Execute when timeout is reached
        Runnable cancelRunnable = () -> {
            task.cancel(true);
            boolean isCancelled = task.isCancelled();
            log.warn("Cancelled {}: {}", taskName, isCancelled);
            if (isCancelled) {
                log.info("Restarting {}...", taskName);
                restartTask.run();
            }
        };

        // Schedule the cancelling just once
        log.debug("Scheduling {} timeout with: {}", taskName, timeout);
        timeoutExecutor = cancelExecutor.schedule(cancelRunnable, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Cancel the timeout executor, if started
     */
    public void cancelTimeout() {
        if(timeoutExecutor != null) timeoutExecutor.cancel(true);
    }
}
