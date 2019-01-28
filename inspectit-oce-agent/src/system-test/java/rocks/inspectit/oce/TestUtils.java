package rocks.inspectit.oce;

import io.opencensus.impl.internal.DisruptorEventQueue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestUtils {

    /**
     * OpenCensus internally manages a queue of events.
     * We simply add an event to the queue and wait until it is processed.
     */
    public static void waitForOpenCensusQueueToBeProcessed() {
        CountDownLatch latch = new CountDownLatch(1);
        DisruptorEventQueue.getInstance().enqueue(latch::countDown);
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

