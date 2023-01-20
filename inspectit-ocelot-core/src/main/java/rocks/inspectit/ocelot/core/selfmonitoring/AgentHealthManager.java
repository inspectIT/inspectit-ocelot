package rocks.inspectit.ocelot.core.selfmonitoring;

import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthIncident;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;
import rocks.inspectit.ocelot.core.utils.RingBuffer;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
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
public class AgentHealthManager {

    private final ApplicationContext ctx;

    private final ScheduledExecutorService executor;

    /**
     * Map of {@code eventClass -> agentHealth}, whereas the {@code agentHealth} is reset whenever an event of type
     * {@code eventClass} occurs (see {@link #onInvalidationEvent(Object)}.
     * The resulting agent health is the most severe value in the map.
     */
    private final Map<Class<?>, AgentHealth> invalidatableHealth = new ConcurrentHashMap<>();

    /**
     * Contains agent health that cannot be invalidated by events. Instead, these health status time out after a
     * defined validity period. The timestamp when that period is over is stored as value.
     */
    private final Map<AgentHealth, LocalDateTime> generalHealthTimeouts = new ConcurrentHashMap<>();

    private AgentHealth lastNotifiedHealth = AgentHealth.OK;

    private final InspectitEnvironment env;

    private final RingBuffer<AgentHealthIncident> healthIncidentBuffer = new RingBuffer<>(10); //TODO make this configurable

    @PostConstruct
    @VisibleForTesting
    void startHealthCheckScheduler() {
        checkHealthAndSchedule();
    }

    public List<AgentHealthIncident> getHistory() {
        return this.healthIncidentBuffer.asList();
    }

    public void update(AgentHealth eventHealth, Class<?> invalidator, String message) {
        if (invalidator == null) {
            handleTimeoutHealth(eventHealth);
        } else {
            handleInvalidatableHealth(eventHealth, invalidator, message);
        }

        triggerEvent(invalidator, message);

    }

    public void handleInvalidatableHealth(AgentHealth eventHealth, Class<?> invalidator, String eventMessage) {
        invalidatableHealth.merge(invalidator, eventHealth, AgentHealth::mostSevere);
        triggerEvent(invalidator, eventMessage);
    }

    private void handleTimeoutHealth(AgentHealth eventHealth) {
        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod();

        if (eventHealth.isMoreSevereOrEqualTo(AgentHealth.WARNING)) {
            generalHealthTimeouts.put(eventHealth, LocalDateTime.now().plus(validityPeriod));
        }
    }

    public void invalidateIncident(Class eventClass, String eventMessage) {
        invalidatableHealth.remove(eventClass);
    }

    private void triggerEvent( Class<?> invalidator, String message) {
        synchronized (this) {
            boolean changedHealth = false;
            AgentHealth currHealth = getCurrentHealth();
            AgentHealth lastHealth = lastNotifiedHealth;
            if(healthChanged()) {
                changedHealth = true;
                lastNotifiedHealth = currHealth;
            }

            AgentHealthIncident incident = new AgentHealthIncident(LocalDateTime.now().toString(), currHealth, invalidator.getTypeName() , message, changedHealth);
            healthIncidentBuffer.put(incident);

            AgentHealthChangedEvent event = new AgentHealthChangedEvent(this, lastHealth, currHealth, message);
            ctx.publishEvent(event);
        }

    }

    private boolean healthChanged() {
        if (getCurrentHealth() != lastNotifiedHealth) {
            AgentHealth currHealth = getCurrentHealth();
            return currHealth != lastNotifiedHealth;
        }
        return false;
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

    /**
     * Checks whether the current health has changed since last check and schedules another check.
     * The next check will run dependent on the earliest status timeout in the future:
     * <ul>
     *     <li>does not exist -> run again after validity period</li>
     *     <li>exists -> run until that timeout is over</li>
     * </ul>validityPeriod
     */
    private void checkHealthAndSchedule() {
        Duration delay = getNextHealthTimeoutDuration();

        if (log.isDebugEnabled()) {
            log.debug("Schedule health check in {} minutes", delay.toMinutes());
        }

        executor.schedule(this::checkHealthAndSchedule, delay.toMillis(), TimeUnit.MILLISECONDS);
    }


    public Duration getNextHealthTimeoutDuration() {
        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod();
        Duration delay = generalHealthTimeouts.values()
                .stream()
                .filter(d -> d.isAfter(LocalDateTime.now()))
                .max(Comparator.naturalOrder())
                .map(d -> Duration.between(d, LocalDateTime.now()).abs())
                .orElse(validityPeriod);

        delay = Duration.ofMillis(Math.max(delay.toMillis(), env.getCurrentConfig()
                .getSelfMonitoring()
                .getAgentHealth()
                .getMinHealthCheckDelay()
                .toMillis()));
        return delay;
    }

}
