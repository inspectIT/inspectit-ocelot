package rocks.inspectit.ocelot.core.selfmonitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the {@link AgentHealth} and publishes {@link AgentHealthChangedEvent}s when it changes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentHealthManager {

    private final ApplicationContext ctx;

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

    public void handleInvalidatableHealth(AgentHealth eventHealth, Class<?> invalidator, String eventMessage) {
        invalidatableHealth.merge(invalidator, eventHealth, AgentHealth::mostSevere);
        triggerEventAndMetricIfHealthChanged(eventMessage);
    }

    private void handleTimeoutHealth(AgentHealth eventHealth) {
        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod();

        if (eventHealth.isMoreSevereOrEqualTo(AgentHealth.WARNING)) {
            generalHealthTimeouts.put(eventHealth, LocalDateTime.now().plus(validityPeriod));
        }
    }

    public void invalidateIncident(Class eventClass, String eventMessage) {
        invalidatableHealth.remove(eventClass);
        triggerEventAndMetricIfHealthChanged(eventMessage);
    }

    private void triggerEventAndMetricIfHealthChanged(String message) {
        if (getCurrentHealth() != lastNotifiedHealth) {
            synchronized (this) {
                AgentHealth currHealth = getCurrentHealth();
                if (currHealth != lastNotifiedHealth) {
                    AgentHealth lastHealth = lastNotifiedHealth;
                    lastNotifiedHealth = currHealth;

                    AgentHealthChangedEvent event = new AgentHealthChangedEvent(this, lastHealth, currHealth, message);
                    ctx.publishEvent(event);
                }
            }
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

    public Duration getNextHealthTimeoutDuration() {
        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod();
        return generalHealthTimeouts.values()
                .stream()
                .filter(d -> d.isAfter(LocalDateTime.now()))
                .max(Comparator.naturalOrder())
                .map(d -> Duration.between(d, LocalDateTime.now()))
                .orElse(validityPeriod);
    }

}
