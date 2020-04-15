package rocks.inspectit.ocelot.core.metrics.percentiles;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.common.Timestamp;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.Tags;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Consumer thread for asynchronously processing measurement observations.
 */
@Slf4j
class AsyncPercentileRecorder {

    private static final int QUEUE_CAPACITY = 8096;

    private PercentileViewManager viewManager;

    private volatile boolean overflowLogged = false;

    private volatile boolean isDestroyed = false;

    @VisibleForTesting
    ArrayBlockingQueue<MetricRecord> recordsQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    @VisibleForTesting
    Thread worker;

    AsyncPercentileRecorder(PercentileViewManager owner) {
        viewManager = owner;
        worker = new Thread(this::doRecord);
        worker.setDaemon(true);
        worker.setName("InspectIT Ocelot percentile Recorder");
        worker.start();
    }

    void recordWithCurrentTagContext(String measureName, double value) {
        if (viewManager.areAnyViewsRegisteredForMeasure(measureName)) {
            boolean success = recordsQueue.offer(new MetricRecord(value, measureName, viewManager.getCurrentTime(),
                    Tags.getTagger().getCurrentTagContext()));
            if (!success && !overflowLogged) {
                overflowLogged = true;
                log.warn("Measurement for percentiles has been dropped because queue is full. This message will not be shown for further drops!");
            }
        }
    }

    void destroy() {
        isDestroyed = true;
        worker.interrupt();
    }

    private void doRecord() {
        while (true) {
            try {
                MetricRecord record = recordsQueue.take();
                viewManager.recordSynchronous(record.measure, record.value, record.time, record.tagContext);
            } catch (InterruptedException e) {
                if (isDestroyed) {
                    return;
                } else {
                    log.error("Unexpected interrupt", e);
                }
            } catch (Exception e) {
                log.error("Error processing record: ", e);
            }
        }
    }

    @Value
    private static class MetricRecord {

        double value;

        String measure;

        Timestamp time;

        TagContext tagContext;

    }

}
