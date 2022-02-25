package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.selfmonitoring.LogPreloadingSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Preloads log events to be used by, e.g., {@link rocks.inspectit.ocelot.core.command.handler.impl.LogsCommandExecutor}.
 * Events are stored in a ring buffer, meaning old events are overwritten when the buffer is full and new events arrive.
 */
@Component
public class LogPreloader {

    private ILoggingEvent[] buffer;

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    @Autowired
    public LogPreloader(InspectitEnvironment env) {
        recreateBufferIfSizeChanged(env.getCurrentConfig().getSelfMonitoring().getLogPreloading());
    }

    /**
     * Records one log event and stores into a local buffer
     * 
     * @param logEvent The log event to record
     */
    public void record(ILoggingEvent logEvent) {
        if (buffer != null) {
            int index = currentIndex.getAndIncrement() % buffer.length;
            try {
                buffer[index] = logEvent;
            } catch (ArrayIndexOutOfBoundsException e) {
                // this may happen while the buffer gets recreated with a smaller size
                // in this case, we just drop the event until everything is properly set again
            }
        }
    }

    /**
     * Returns the preloaded logs as iterable.
     * Returns all logs that are preloaded at the time of calling this method,
     * i.e., logs inserted after that time will not be contained.
     *
     * @return An iterable of the preloaded logs
     */
    public Iterable<ILoggingEvent> getPreloadedLogs() {
        return () -> new BufferIterator(buffer, currentIndex.get() - 1);
    }

    @PostConstruct
    private void subscribe() {
        LogPreloadingAppender.registerPreloader(this);
    }

    @EventListener
    @VisibleForTesting
    void inspectitConfigurationChanged(InspectitConfigChangedEvent event) {
        recreateBufferIfSizeChanged(event.getNewConfig().getSelfMonitoring().getLogPreloading());
    }

    /**
     * Recreates the buffer array and tries to re-insert old values into the new array.
     * Drops all previously collected logs.
     *
     * @param settings The LogPreloadingSettings
     */
    private void recreateBufferIfSizeChanged(LogPreloadingSettings settings) {
        if (settings != null && (buffer == null || buffer.length != settings.getBufferSize())) {
            buffer = new ILoggingEvent[settings.getBufferSize()];
            // few log entries might be written to arbitrary indices between these two code lines,
            // meaning they are lost as well
            currentIndex.set(0);
        }
    }

    private class BufferIterator implements Iterator<ILoggingEvent> {

        private final ILoggingEvent[] buffer;

        private final int targetIndex;

        private int currentIndex;

        /**
         * Creates a new iterator.
         *
         * @param buffer      The buffer, which will be copied
         * @param targetIndex The maximum index to be returned
         */
        private BufferIterator(ILoggingEvent[] buffer, int targetIndex) {
            this.buffer = new ILoggingEvent[buffer.length];
            System.arraycopy(buffer, 0, this.buffer, 0, buffer.length);
            this.targetIndex = targetIndex;
            currentIndex = targetIndex >= buffer.length ? targetIndex - buffer.length + 1 : 0;
        }

        @Override
        public boolean hasNext() {
            return currentIndex <= targetIndex;
        }

        @Override
        public ILoggingEvent next() {
            ILoggingEvent next = buffer[currentIndex % buffer.length];
            currentIndex++;
            return next;
        }

    }

}
