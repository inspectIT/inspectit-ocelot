package rocks.inspectit.oce.core.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * Service to schedule asynchronous tasks which are executed in batches.
 */
@Service
@Slf4j
public class BatchJobExecutorService {

    @Autowired
    private ScheduledExecutorService executor;

    @FunctionalInterface
    public interface BatchProcessor {

        /**
         * When called this method should execute a batch of (at maximum) the given size.
         * The methods return value specifies if further processing is required.
         *
         * @param batchSize the maximum size of the batch to process
         * @return true, if the job is complete (= no future batches will be scheduled)
         */
        boolean processBatch(int batchSize);
    }

    /**
     * A batch job whose batch size and the inter-batch delay can be reconfigured dynamically.
     */
    public class BatchJob {

        private BatchProcessor processorToExecute;

        /**
         * The size of the batches to process, can be changed while executing.
         */
        @Setter
        private int batchSizes;

        /**
         * The pause between the batches, can be changed while executing.
         */
        @Setter
        private Duration interBatchDelay;

        private BatchJob(BatchProcessor processorToExecute, int batchSizes, Duration interBatchDelay) {
            this.processorToExecute = processorToExecute;
            this.batchSizes = batchSizes;
            this.interBatchDelay = interBatchDelay;
        }

        private Runnable runner = () -> {
            boolean done = false;
            try {
                done = processorToExecute.processBatch(batchSizes);
            } catch (Exception e) {
                log.error("Error processing batch!", e);
            }
            if (!done) {
                scheduleNextBatch(interBatchDelay);
            }
        };

        private void scheduleNextBatch(Duration delay) {
            executor.schedule(runner, delay.toNanos(), TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Starts a new batch job
     *
     * @param task            the actual job to process
     * @param batchSizes      the size of the batches
     * @param startDelay      the time to wait until the execution of the first batch
     * @param interBatchDelay the pause between the batches
     * @return the started {@link BatchJob}
     */
    public BatchJob startJob(BatchProcessor task, int batchSizes, Duration startDelay, Duration interBatchDelay) {
        BatchJob job = new BatchJob(task, batchSizes, interBatchDelay);
        job.scheduleNextBatch(startDelay);
        return job;
    }


    /**
     * Starts a new batch job which runs infinitely;
     *
     * @param task            the actual job to process
     * @param batchSizes      the size of the batches
     * @param startDelay      the time to wait until the execution of the first batch
     * @param interBatchDelay the pause between the batches
     * @return the started {@link BatchJob}
     */
    public BatchJob startJob(IntConsumer task, int batchSizes, Duration startDelay, Duration interBatchDelay) {
        return startJob((batch) -> {
            task.accept(batch);
            return false;
        }, batchSizes, startDelay, interBatchDelay);
    }
}
