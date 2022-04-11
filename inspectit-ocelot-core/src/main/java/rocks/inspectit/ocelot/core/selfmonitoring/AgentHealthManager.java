package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.logging.logback.InternalProcessingAppender;
import rocks.inspectit.ocelot.core.selfmonitoring.event.AgentHealthChangedEvent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the {@link AgentHealth} and publishes {@link AgentHealthChangedEvent}s when it changes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentHealthManager implements InternalProcessingAppender.LogEventConsumer {

    private static final String LOG_CHANGE_STATUS = "The agent status changed from {} to {}.";

    private final ApplicationContext ctx;

    private final ScheduledExecutorService executor;

    private final InspectitEnvironment env;

    private final SelfMonitoringService selfMonitoringService;

    private final Map<Class<?>, AgentHealth> invalidatableHealth = new ConcurrentHashMap<>();

    private final Map<AgentHealth, LocalDateTime> generalHealthTimeouts = new ConcurrentHashMap<>();

    private AgentHealth lastNotifiedHealth = AgentHealth.OK;

    @Override
    public void onLoggingEvent(ILoggingEvent event, Class<?> invalidator) {
        if (AgentHealthManager.class.getCanonicalName().equals(event.getLoggerName())) {
            // ignore own logs, which otherwise would tend to cause infinite loops
            return;
        }

        AgentHealth eventHealth = AgentHealth.fromLogLevel(event.getLevel());

        if (invalidator == null) {
            handleTimeoutHealth(eventHealth);
        } else {
            handleInvalidatableHealth(eventHealth, invalidator);
        }

        triggerEventAndMetricIfHealthChanged();
    }

    private void handleInvalidatableHealth(AgentHealth eventHealth, Class<?> invalidator) {
        invalidatableHealth.merge(invalidator, eventHealth, AgentHealth::mostSevere);
    }

    private void handleTimeoutHealth(AgentHealth eventHealth) {
        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod();

        if (eventHealth.isMoreSevereOrEqualTo(AgentHealth.WARNING)) {
            generalHealthTimeouts.put(eventHealth, LocalDateTime.now().plus(validityPeriod));
        }
    }

    /**
     * Returns the current agent health, which is the most severe out of instrumentation and general status.
     *
     * @return The current agent health
     */
    public AgentHealth getCurrentHealth() {
        AgentHealth generalHealth = generalHealthTimeouts.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().isAfter(LocalDateTime.now()))
                .map(Map.Entry::getKey)
                .max(Comparator.naturalOrder())
                .orElse(AgentHealth.OK);

        AgentHealth invHealth = invalidatableHealth.values()
                .stream()
                .reduce(AgentHealth::mostSevere)
                .orElse(AgentHealth.OK);

        return AgentHealth.mostSevere(generalHealth, invHealth);
    }

    @Override
    public void onInvalidationEvent(Object invalidator) {
        invalidatableHealth.remove(invalidator.getClass());
        triggerEventAndMetricIfHealthChanged();
    }

    @PostConstruct
    @VisibleForTesting
    void registerAtAppender() {
        InternalProcessingAppender.register(this);
    }

    @PostConstruct
    @VisibleForTesting
    void startHealthCheckScheduler() {
        checkHealthAndSchedule();
    }

    @PostConstruct
    @VisibleForTesting
    void sendInitialHealthMetric() {
        selfMonitoringService.recordMeasurement("health", AgentHealth.OK.ordinal());
    }

    @PreDestroy
    @VisibleForTesting
    void unregisterFromAppender() {
        InternalProcessingAppender.unregister(this);
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
        if (getCurrentHealth() != lastNotifiedHealth) {
            synchronized (this) {
                AgentHealth currHealth = getCurrentHealth();
                if (currHealth != lastNotifiedHealth) {
                    AgentHealth lastHealth = lastNotifiedHealth;
                    lastNotifiedHealth = currHealth;
                    if (currHealth.isMoreSevereOrEqualTo(lastHealth)) {
                        log.warn(LOG_CHANGE_STATUS, lastHealth, currHealth);
                    } else {
                        log.info(LOG_CHANGE_STATUS, lastHealth, currHealth);
                    }

                    selfMonitoringService.recordMeasurement("health", currHealth.ordinal());

                    AgentHealthChangedEvent event = new AgentHealthChangedEvent(this, lastHealth, currHealth);
                    ctx.publishEvent(event);
                }
            }
        }
    }
}
