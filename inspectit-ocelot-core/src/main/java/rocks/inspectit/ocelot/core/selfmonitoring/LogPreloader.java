package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.status.AgentStatus;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.selfmonitoring.LogPreloadingSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Preloads log events to be used by, e.g., {@link rocks.inspectit.ocelot.core.command.handler.impl.LogsCommandExecutor}.
 * Events are stored in a ring buffer, meaning old events are overwritten when the buffer is full and new events arrive.
 */
@Component
@Slf4j
public class LogPreloader extends DynamicallyActivatableService {

    private static final String LOG_CHANGE_STATUS = "Changing the agent status from {} to {}.";

    private ILoggingEvent[] buffer;

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    private final ApplicationContext ctx;

    private Optional<AgentStatus> status = Optional.empty();

    @Autowired
    public LogPreloader(ApplicationContext ctx) {
        super("logPreloading");
        this.ctx = ctx;
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

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        return configuration.getLogPreloading() != null && configuration.getLogPreloading().isEnabled();
    }

    /**
     * Recreates the buffer array if the configured size changed.
     * Drops all previously collected logs.
     *
     * @param configuration the configuration used to start the service. Is the same configuration as {@link InspectitEnvironment#getCurrentConfig()}.
     *
     * @return true if the enabling was successful, false otherwise.
     */
    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        LogPreloadingSettings settings = configuration.getLogPreloading();

        if (settings != null && (buffer == null || buffer.length != settings.getBufferSize())) {
            if (settings.getBufferSize() < 1) {
                log.error("Cannot enable LogPreloader with configured buffer size {}!", settings.getBufferSize());
                return false;
            } else {
                log.info("Enabling LogPreloader with buffer size {}.", settings.getBufferSize());
                buffer = new ILoggingEvent[settings.getBufferSize()];
                // few log entries might be written to arbitrary indices between these two code lines,
                // meaning they are lost as well
                currentIndex.set(0);
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean doDisable() {
        log.info("Disabling LogPreloader. All previously preloaded logs are dropped.");
        buffer = null;
        return true;
    }

    // TODO: call it!
    private void updateStatus(@NonNull AgentStatus newStatus) {
        if (newStatus != status.get()) {
            String oldStatusName = status.map(AgentStatus::name).orElseGet(() -> "none");
            if (newStatus.isMoreSevereOrEqualTo(AgentStatus.WARNING)) {
                log.warn(LOG_CHANGE_STATUS, oldStatusName, newStatus);
            } else {
                log.info(LOG_CHANGE_STATUS, oldStatusName, newStatus);
            }

            Optional<AgentStatus> oldStatus = status;
            status = Optional.of(newStatus);
            sendAgentStatusChangedEvent(oldStatus, newStatus);
        }
    }

    private void sendAgentStatusChangedEvent(Optional<AgentStatus> oldStatus, AgentStatus newStatus) {
        AgentStatusChangedEvent event = new AgentStatusChangedEvent(this, oldStatus, newStatus);
        ctx.publishEvent(event);
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
            this.buffer = new ILoggingEvent[buffer != null ? buffer.length : 0];
            this.targetIndex = targetIndex;

            if (buffer != null) {
                System.arraycopy(buffer, 0, this.buffer, 0, buffer.length);
                currentIndex = targetIndex >= buffer.length ? targetIndex - buffer.length + 1 : 0;
            }
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
