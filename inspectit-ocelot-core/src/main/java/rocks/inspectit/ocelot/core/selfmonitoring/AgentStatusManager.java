package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.status.AgentStatus;
import rocks.inspectit.ocelot.core.logging.logback.InternalProcessingAppender;
import rocks.inspectit.ocelot.core.selfmonitoring.event.AgentStatusChangedEvent;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class AgentStatusManager implements InternalProcessingAppender.Observer {

    private static final String LOG_CHANGE_STATUS = "Changing the agent status from {} to {}.";

    private final ApplicationContext ctx;

    private final ScheduledExecutorService executor;

    private AgentStatus instrumentationStatus = AgentStatus.OK;

    private final Map<AgentStatus, LocalDateTime> generalStatusTimeouts = new ConcurrentHashMap<>();

    private AgentStatus lastNotifiedStatus = AgentStatus.OK;

    // TODO: reset instrumentationStatus when instrumentation is newly triggered

    @PostConstruct
    private void startEventTrigger() {
        long notificationIntervalMillis = 42; // TODO: read from config
        executor.scheduleWithFixedDelay(this::triggerEventIfStatusChanged, notificationIntervalMillis, notificationIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onInstrumentationLoggingEvent(ILoggingEvent event) {
        instrumentationStatus = AgentStatus.mostSevere(instrumentationStatus, AgentStatus.fromLogLevel(event.getLevel()));
        triggerEventIfStatusChanged();
    }

    @Override
    public void onGeneralLoggingEvent(ILoggingEvent event) {
        AgentStatus eventStatus = AgentStatus.fromLogLevel(event.getLevel());
        Duration statusTimeout = Duration.ofMinutes(30); // TODO: read from config

        if (eventStatus.isMoreSevereOrEqualTo(AgentStatus.WARNING)) {
            generalStatusTimeouts.put(eventStatus, LocalDateTime.now().plus(statusTimeout));
        }

        triggerEventIfStatusChanged();
    }

    /**
     * Returns the current agent status, which is the most severe out of instrumentation and general status.
     *
     * @return The current agent status
     */
    public AgentStatus getCurrentStatus() {
        Optional<AgentStatus> generalStatus = generalStatusTimeouts.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().isBefore(LocalDateTime.now()))
                .map(Map.Entry::getKey)
                .max(Comparator.naturalOrder());

        return AgentStatus.mostSevere(instrumentationStatus, generalStatus.orElse(AgentStatus.OK));
    }

    private void triggerEventIfStatusChanged() {
        AgentStatus currStatus = getCurrentStatus();
        if (currStatus != lastNotifiedStatus) {
            if (currStatus.isMoreSevereOrEqualTo(AgentStatus.WARNING)) {
                log.warn(LOG_CHANGE_STATUS, lastNotifiedStatus, currStatus);
            } else {
                log.info(LOG_CHANGE_STATUS, lastNotifiedStatus, currStatus);
            }

            AgentStatusChangedEvent event = new AgentStatusChangedEvent(this, lastNotifiedStatus, currStatus);
            ctx.publishEvent(event);
            lastNotifiedStatus = currStatus;
        }
    }
}
