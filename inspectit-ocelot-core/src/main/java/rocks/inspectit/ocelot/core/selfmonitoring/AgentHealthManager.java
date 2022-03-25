package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.logging.logback.InternalProcessingAppender;
import rocks.inspectit.ocelot.core.selfmonitoring.event.AgentHealthChangedEvent;

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
 * Manages the {@link AgentHealth} and publishes {@link AgentHealthChangedEvent}s when it changes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentHealthManager implements InternalProcessingAppender.Observer {

    private static final String LOG_CHANGE_STATUS = "The agent status changed from {} to {}.";

    private final ApplicationContext ctx;

    private final ScheduledExecutorService executor;

    private final InspectitEnvironment env;

    private final SelfMonitoringService selfMonitoringService;

    private AgentHealth instrumentationHealth = AgentHealth.OK;

    private final Map<AgentHealth, LocalDateTime> generalHealthTimeouts = new ConcurrentHashMap<>();

    private AgentHealth lastNotifiedHealth = AgentHealth.OK;

    @Override
    public void onInstrumentationLoggingEvent(ILoggingEvent event) {
        instrumentationHealth = AgentHealth.mostSevere(instrumentationHealth, AgentHealth.fromLogLevel(event.getLevel()));
        triggerEventAndMetricIfHealthChanged();
    }

    @Override
    public void onGeneralLoggingEvent(ILoggingEvent event) {
        AgentHealth eventHealth = AgentHealth.fromLogLevel(event.getLevel());
        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod();

        if (eventHealth.isMoreSevereOrEqualTo(AgentHealth.WARNING)) {
            generalHealthTimeouts.put(eventHealth, LocalDateTime.now().plus(validityPeriod));
        }

        triggerEventAndMetricIfHealthChanged();
    }

    /**
     * Returns the current agent health, which is the most severe out of instrumentation and general status.
     *
     * @return The current agent health
     */
    public AgentHealth getCurrentHealth() {
        Optional<AgentHealth> generalStatus = generalHealthTimeouts.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().isAfter(LocalDateTime.now()))
                .map(Map.Entry::getKey)
                .max(Comparator.naturalOrder());

        return AgentHealth.mostSevere(instrumentationHealth, generalStatus.orElse(AgentHealth.OK));
    }

    @EventListener
    @VisibleForTesting
    private void resetInstrumentationHealth(InstrumentationConfigurationChangedEvent ev) {
        instrumentationHealth = AgentHealth.OK;
        triggerEventAndMetricIfHealthChanged();
    }

    @PostConstruct
    private void startHealthManager() {
        InternalProcessingAppender.register(this);
        checkHealthAndSchedule();
    }

    /**
     * Checks whether the current health has changed since last check and schedules another check.
     * The next check will run dependent on the earliest status timeout in the future:
     * <ul>
     *     <li>does not exist -> run again after validity period</li>
     *     <li>exists -> run until that timeout is over</li>
     * </ul>
     */
    private void checkHealthAndSchedule() {
        triggerEventAndMetricIfHealthChanged();

        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod();
        Duration delay = generalHealthTimeouts.values()
                .stream()
                .filter(d -> d.isAfter(LocalDateTime.now()))
                .max(Comparator.naturalOrder())
                .map(d -> Duration.between(d, LocalDateTime.now()))
                .orElse(validityPeriod);

        executor.schedule(this::checkHealthAndSchedule, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void triggerEventAndMetricIfHealthChanged() {
        AgentHealth currHealth = getCurrentHealth();
        if (currHealth != lastNotifiedHealth) {
            if (currHealth.isMoreSevereOrEqualTo(AgentHealth.WARNING)) {
                log.warn(LOG_CHANGE_STATUS, lastNotifiedHealth, currHealth);
            } else {
                log.info(LOG_CHANGE_STATUS, lastNotifiedHealth, currHealth);
            }

            selfMonitoringService.recordMeasurement("health", currHealth.ordinal());

            AgentHealthChangedEvent event = new AgentHealthChangedEvent(this, lastNotifiedHealth, currHealth);
            ctx.publishEvent(event);
            lastNotifiedHealth = currHealth;
        }
    }
}
