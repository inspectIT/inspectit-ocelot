package rocks.inspectit.oce;

import io.opencensus.impl.internal.DisruptorEventQueue;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestUtils {

    /**
     * Opencensus internally manages a queue of events.
     * We simply add a event to the queue and wait unti it is processed.
     */
    public static void waitForOpenCensusQueueToBeProcessed() {
        AtomicBoolean entryProcessed = new AtomicBoolean(false);
        DisruptorEventQueue.getInstance().enqueue(() -> {
            synchronized (entryProcessed) {
                entryProcessed.set(true);
                entryProcessed.notify();
            }
        });
        synchronized (entryProcessed) {
            while (!entryProcessed.get()) {
                try {
                    entryProcessed.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

