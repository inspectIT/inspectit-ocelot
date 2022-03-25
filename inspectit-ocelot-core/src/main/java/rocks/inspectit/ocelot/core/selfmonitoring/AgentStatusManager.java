package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.status.AgentStatus;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
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

/**
 * Manages the {@link AgentStatus} and publishes {@link AgentStatusChangedEvent}s when it changes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentStatusManager implements InternalProcessingAppender.Observer {

    private static final String LOG_CHANGE_STATUS = "The agent status changed from {} to {}.";

    private final ApplicationContext ctx;

    private final ScheduledExecutorService executor;

    private final InspectitEnvironment env;

    private final SelfMonitoringService selfMonitoringService;

    private AgentStatus instrumentationStatus = AgentStatus.OK;

    private final Map<AgentStatus, LocalDateTime> generalStatusTimeouts = new ConcurrentHashMap<>();

    private AgentStatus lastNotifiedStatus = AgentStatus.OK;

    @Override
    public void onInstrumentationLoggingEvent(ILoggingEvent event) {
        instrumentationStatus = AgentStatus.mostSevere(instrumentationStatus, AgentStatus.fromLogLevel(event.getLevel()));
        triggerEventAndMetricIfStatusChanged();
    }

    @Override
    public void onGeneralLoggingEvent(ILoggingEvent event) {
        AgentStatus eventStatus = AgentStatus.fromLogLevel(event.getLevel());
        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentStatus().getValidityPeriod();

        if (eventStatus.isMoreSevereOrEqualTo(AgentStatus.WARNING)) {
            generalStatusTimeouts.put(eventStatus, LocalDateTime.now().plus(validityPeriod));
        }

        triggerEventAndMetricIfStatusChanged();
    }

    /**
     * Returns the current agent status, which is the most severe out of instrumentation and general status.
     *
     * @return The current agent status
     */
    public AgentStatus getCurrentStatus() {
        Optional<AgentStatus> generalStatus = generalStatusTimeouts.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().isAfter(LocalDateTime.now()))
                .map(Map.Entry::getKey)
                .max(Comparator.naturalOrder());

        return AgentStatus.mostSevere(instrumentationStatus, generalStatus.orElse(AgentStatus.OK));
    }

    @EventListener
    @VisibleForTesting
    private void resetInstrumentationStatus(InstrumentationConfigurationChangedEvent ev) {
        instrumentationStatus = AgentStatus.OK;
        triggerEventAndMetricIfStatusChanged();
    }

    @PostConstruct
    private void startEventTrigger() {
        checkStateAndSchedule();
    }

    /**
     * Checks whether the current state has changed since last check and schedules another check.
     * The next check will run dependent on the earliest status timeout in the future:
     * <ul>
     *     <li>does not exist -> run again after validity period</li>
     *     <li>exists -> run until that timeout is over</li>
     * </ul>
     */
    private void checkStateAndSchedule() {
        triggerEventAndMetricIfStatusChanged();

        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentStatus().getValidityPeriod();
        Duration delay = generalStatusTimeouts.values()
                .stream()
                .filter(d -> d.isAfter(LocalDateTime.now()))
                .max(Comparator.naturalOrder())
                .map(d -> Duration.between(d, LocalDateTime.now()))
                .orElse(validityPeriod);

        executor.schedule(this::checkStateAndSchedule, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void triggerEventAndMetricIfStatusChanged() {
        AgentStatus currStatus = getCurrentStatus();
        if (currStatus != lastNotifiedStatus) {
            if (currStatus.isMoreSevereOrEqualTo(AgentStatus.WARNING)) {
                log.warn(LOG_CHANGE_STATUS, lastNotifiedStatus, currStatus);
            } else {
                log.info(LOG_CHANGE_STATUS, lastNotifiedStatus, currStatus);
            }

            selfMonitoringService.recordMeasurement("status", currStatus.ordinal());

            AgentStatusChangedEvent event = new AgentStatusChangedEvent(this, lastNotifiedStatus, currStatus);
            ctx.publishEvent(event);
            lastNotifiedStatus = currStatus;
        }
    }
}
