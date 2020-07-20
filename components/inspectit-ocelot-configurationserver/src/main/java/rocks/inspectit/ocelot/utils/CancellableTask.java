package rocks.inspectit.ocelot.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public abstract class CancellableTask<R> implements Runnable {

    /**
     * Internal flag to check if cancel has been called.
     */
    private volatile boolean cancelFlag = false;

    /**
     * Callback which is invoked when this task has finished.
     */
    private Consumer<R> onLoadCallback;

    protected CancellableTask(Consumer<R> onLoadCallback) {
        this.onLoadCallback = onLoadCallback;
    }

    /**
     * Can be invoked to cancel this task.
     * As soon as this method returns, it is guaranteed that the configured onLoad-callback will not be invoked anymore.
     */
    public final synchronized void cancel() {
        cancelFlag = true;
    }

    /**
     * @return true, if {@link #cancel()} was called.
     */
    public final boolean isCanceled() {
        return cancelFlag;
    }

    /**
     * Should be invoked by the {@link #run()} method when this task has finished.
     * This guarantees that {@link #onLoadCallback} is only invoked if the task has not been canceled.
     *
     * @param result the result of the task, which will be provided to the configured {@link #onLoadCallback}.
     */
    protected final void onTaskSuccess(R result) {
        synchronized (this) {
            if (cancelFlag) {
                log.debug("{} canceled", getClass().getSimpleName());
                return;
            }
            onLoadCallback.accept(result);
            log.info("{} successfully finished", getClass().getSimpleName());
        }
    }
}
