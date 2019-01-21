package rocks.inspectit.oce.core.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service to schedule asynchronous tasks which are executed in batches.
 */
@Service
@Slf4j
public class BatchJobExecutorService {

    @Autowired
    private ScheduledExecutorService executor;

    @FunctionalInterface
    public interface BatchProcessor<T> {

        /**
         * When called this method should execute a batch of (at maximum) the given size.
         * The methods return value specifies if further processing is required.
         *
         * @param batchSize the maximum size of the batch to process
         * @return true, if the job is complete (= no future batches will be scheduled)
         */
        boolean processBatch(T batchSize);
    }

    /**
     * A batch job whose batch size and the inter-batch delay can be reconfigured dynamically.
     */
    public class BatchJob<T> {

        private BatchProcessor<T> processorToExecute;

        /**
         * The size of the batches to process, can be changed while executing.
         */
        @Setter
        private T batchSizes;

        /**
         * The pause between the batches, can be changed while executing.
         */
        @Setter
        private Duration interBatchDelay;

        /**
         * True if this job has been canceled.
         * Note that even if this job was canceled, it is not guaranteed that the last previously scheduled batch is not processed.
         */
        @Getter
        private volatile boolean canceled = false;

        private Future<?> nextScheduledBatch;

        private BatchJob(BatchProcessor<T> processorToExecute, T batchSizes, Duration interBatchDelay) {
            this.processorToExecute = processorToExecute;
            this.batchSizes = batchSizes;
            this.interBatchDelay = interBatchDelay;
        }

        /**
         * Cancels this BatchJob, meaning that no future batches will be scheduled for executing.
         */
        public void cancel() {
            canceled = true;
            nextScheduledBatch.cancel(false);
        }

        private Runnable runner = () -> {
            boolean done = false;
            try {
                done = processorToExecute.processBatch(batchSizes);
            } catch (Exception e) {
                log.error("Error processing batch!", e);
            }
            if (!done && !canceled) {
                scheduleNextBatch(interBatchDelay);
            }
        };

        private void scheduleNextBatch(Duration delay) {
            nextScheduledBatch = executor.schedule(runner, delay.toNanos(), TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Starts a new batch job
     *
     * @param <T>             the batch size type, e.g. an Integer
     * @param task            the actual job to process
     * @param batchSizes      the size of the batches
     * @param startDelay      the time to wait until the execution of the first batch
     * @param interBatchDelay the pause between the batches
     * @return the started {@link BatchJob}
     */
    public <T> BatchJob<T> startJob(BatchProcessor<T> task, T batchSizes, Duration startDelay, Duration interBatchDelay) {
        BatchJob job = new BatchJob(task, batchSizes, interBatchDelay);
        job.scheduleNextBatch(startDelay);
        return job;
    }


    /**
     * Starts a new batch job which runs infinitely;
     *
     * @param <T>             the batch size type, e.g. an Integer
     * @param task            the actual job to process
     * @param batchSizes      the size of the batches
     * @param startDelay      the time to wait until the execution of the first batch
     * @param interBatchDelay the pause between the batches
     * @return the started {@link BatchJob}
     */
    public <T> BatchJob<T> startJob(Consumer<T> task, T batchSizes, Duration startDelay, Duration interBatchDelay) {
        return startJob((batch) -> {
            task.accept(batch);
            return false;
        }, batchSizes, startDelay, interBatchDelay);
    }
}
