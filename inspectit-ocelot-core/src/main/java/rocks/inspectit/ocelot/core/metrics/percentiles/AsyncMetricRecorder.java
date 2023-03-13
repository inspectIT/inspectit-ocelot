package rocks.inspectit.ocelot.core.metrics.percentiles;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.common.Timestamp;
import io.opencensus.tags.TagContext;
import io.opentelemetry.api.common.Attributes;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Consumer thread for asynchronously processing measurement observations.
 */
@Slf4j
class AsyncMetricRecorder {

    private static final int QUEUE_CAPACITY = 8096;

    private final MetricConsumer metricConsumer;

    private volatile boolean overflowLogged = false;

    private volatile boolean isDestroyed = false;

    @VisibleForTesting
    final ArrayBlockingQueue<MetricRecord> recordsQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    @VisibleForTesting
    final Thread worker;

    AsyncMetricRecorder(MetricConsumer consumer) {
        metricConsumer = consumer;
        worker = new Thread(this::doRecord);
        worker.setDaemon(true);
        worker.setName("InspectIT Ocelot percentile Recorder");
        worker.start();
    }

    void record(String measureName, double value, Timestamp time, TagContext tags, Attributes attributes) {
        // OC
        if (null != tags) {
            record(measureName, value, time, tags);
        }
        // OTEL
        if (null != attributes) {
            record(measureName, value, time, attributes);
        }
    }

    void record(String measureName, double value, Timestamp time, TagContext tags) {
        boolean success = recordsQueue.offer(new MetricRecord(value, measureName, time, tags));
        if (!success && !overflowLogged) {
            overflowLogged = true;
            log.warn("Measurement for percentiles has been dropped because queue is full. This message will not be shown for further drops!");
        }
    }

    public void record(String measureName, double value, Timestamp time, Attributes attributes) {
        boolean success = recordsQueue.offer(new MetricRecord(value, measureName, time, attributes));
        if (!success && !overflowLogged) {
            overflowLogged = true;
            log.warn("Measurement for percentiles has been dropped because queue is full. This message will not be shown for further drops!");
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
                metricConsumer.record(record.measure, record.value, record.time, record.tagContext, record.attributes);
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

    public interface MetricConsumer {

        void record(String measure, double value, Timestamp time, TagContext tags, Attributes attributes);
    }

    @Value
    private static class MetricRecord {

        double value;

        String measure;

        Timestamp time;

        TagContext tagContext;

        Attributes attributes;

        public MetricRecord(double value, String measureName, Timestamp time, Attributes attributes) {
            this.value = value;
            measure = measureName;
            this.time = time;
            this.attributes = attributes;
            tagContext = null;
        }

        public MetricRecord(double value, String measureName, Timestamp time, TagContext tagContext) {
            this.value = value;
            measure = measureName;
            this.time = time;
            this.tagContext = tagContext;
            attributes = null;
        }
    }

}
