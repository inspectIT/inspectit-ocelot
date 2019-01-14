package rocks.inspectit.oce.core.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service to schedule asynchronous tasks which are executed in batches.
 */
@Service
@Slf4j
public class BatchJobExecutorService {

    @Autowired
    private ScheduledExecutorService executor;

    public interface BatchProcessor {

        /**
         * @param batchSize the maximum size of the batch to process
         * @return true, if the job is complete (= no future batches will be scheduled)
         */
        boolean processBatch(int batchSize);
    }

    public class BatchJob {

        BatchProcessor processorToExecute;

        @Setter
        private int batchSizes;

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

    public BatchJob startJob(BatchProcessor task, int batchSizes, Duration startDelay, Duration interBatchDelay) {
        BatchJob job = new BatchJob(task, batchSizes, interBatchDelay);
        job.scheduleNextBatch(startDelay);
        return job;
    }
}
