package rocks.inspectit.ocelot.core.selfmonitoring;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthIncident;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

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
public class AgentHealthManager {

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private ScheduledExecutorService executor;
    @Autowired
    private InspectitEnvironment env;
    @Autowired
    private AgentHealthIncidentBuffer healthIncidentBuffer;

    /**
     * Map of {@code eventClass -> agentHealth}, whereas the {@code agentHealth} is reset whenever an event of type
     * {@code eventClass} occurs (see {@link #onInvalidationEvent(Object)}).
     * The resulting agent health is the most severe value in the map.
     */
    private final Map<Class<?>, AgentHealth> invalidatableHealth = new ConcurrentHashMap<>();

    /**
     * Contains agent health that cannot be invalidated by events. Instead, these health status time out after a
     * defined validity period. The timestamp when that period is over is stored as value.
     */
    private final Map<AgentHealth, LocalDateTime> generalHealthTimeouts = new ConcurrentHashMap<>();

    private AgentHealth lastNotifiedHealth = AgentHealth.OK;

    @PostConstruct
    @VisibleForTesting
    void startHealthCheckScheduler() {
        checkHealthAndSchedule();
    }

    public List<AgentHealthIncident> getIncidentHistory() {
        return healthIncidentBuffer.asList();
    }

    /**
     * Notifies the AgentHealthManager about an eventHealth.
     * The manager determines, whether the event is invalidatable or times out.
     *
     * @param eventHealth health of event
     * @param invalidator class, which created the invalidatable eventHealth
     * @param loggerName name of the logger, who created the event
     * @param message message of the event
     */
    public void notifyAgentHealth(AgentHealth eventHealth, Class<?> invalidator, String loggerName, String message) {
        if (invalidator == null)
            handleTimeoutHealth(eventHealth, loggerName, message);
         else
            handleInvalidatableHealth(eventHealth, invalidator, message);
    }

    private void handleInvalidatableHealth(AgentHealth eventHealth, Class<?> invalidator, String eventMessage) {
        invalidatableHealth.merge(invalidator, eventHealth, AgentHealth::mostSevere);

        boolean shouldCreateIncident = eventHealth.isMoreSevereOrEqualTo(AgentHealth.WARNING);
        triggerAgentHealthChangedEvent(invalidator.getTypeName(), eventMessage, shouldCreateIncident);
    }

    private void handleTimeoutHealth(AgentHealth eventHealth, String loggerName, String eventMassage) {
        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod();
        boolean isNotInfo = eventHealth.isMoreSevereOrEqualTo(AgentHealth.WARNING);

        if (isNotInfo) {
            generalHealthTimeouts.put(eventHealth, LocalDateTime.now().plus(validityPeriod));
        }
        String fullEventMessage = eventMassage + ". This status is valid for " + validityPeriod;
        triggerAgentHealthChangedEvent(loggerName, fullEventMessage, isNotInfo);
    }

    /**
     * Invalidates an invalidatable eventHealth and creates a new AgentHealthIncident
     * @param eventClass class, which created the invalidatable eventHealth
     * @param eventMessage message of the event
     */
    public void invalidateIncident(Class<?> eventClass, String eventMessage) {
        invalidatableHealth.remove(eventClass);
        triggerAgentHealthChangedEvent(eventClass.getTypeName(), eventMessage);
    }

    /**
     * Checks whether the current health has changed since last check and schedules another check.
     * The next check will run dependent on the earliest status timeout in the future:
     * <ul>
     *     <li>does not exist -> run again after validity period</li>
     *     <li>exists -> run until that timeout is over</li>
     * </ul>
     */
    @VisibleForTesting
    void checkHealthAndSchedule() {
        triggerAgentHealthChangedEvent(AgentHealthManager.class.getCanonicalName(), "Checking timed out agent healths");

        Duration validityPeriod = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod();
        Duration minDelay = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getMinHealthCheckDelay();
        Duration delay = generalHealthTimeouts.values()
                .stream()
                .filter(dateTime -> dateTime.isAfter(LocalDateTime.now()))
                .max(Comparator.naturalOrder())
                .map(dateTime -> {
                    Duration dif = Duration.between(dateTime, LocalDateTime.now());
                    if (minDelay.compareTo(dif) > 0) return minDelay;
                    else return dif;
                })
                .orElse(validityPeriod);

        executor.schedule(this::checkHealthAndSchedule, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new AgentHealthIncident, if specified, and also triggers an AgentHealthChangedEvent,
     * if the agent health has changed
     *
     * @param incidentSource class, which caused the incident
     * @param message message, describing the incident
     * @param shouldCreateIncident whether to create a new AgentHealthIncident or not
     */
    private void triggerAgentHealthChangedEvent(String incidentSource, String message, Boolean shouldCreateIncident) {
        synchronized (this) {
            boolean changedHealth = healthHasChanged();
            AgentHealth currentHealth = getCurrentHealth();

            if(shouldCreateIncident) {
                AgentHealthIncident incident = new AgentHealthIncident(
                        LocalDateTime.now().toString(), currentHealth, incidentSource, message, changedHealth);
                healthIncidentBuffer.put(incident);
            }

            if(changedHealth) {
                AgentHealth lastHealth = lastNotifiedHealth;
                lastNotifiedHealth = currentHealth;
                AgentHealthChangedEvent event = new AgentHealthChangedEvent(this, lastHealth, currentHealth, message);
                ctx.publishEvent(event);
            }
        }
    }

    private void triggerAgentHealthChangedEvent(String incidentSource, String message) {
        triggerAgentHealthChangedEvent(incidentSource, message, true);
    }

    /**
     * Checks whether the current health has changed since last check.
     *
     * @return true if the health state changed
     */
    private boolean healthHasChanged() {
        return getCurrentHealth() != lastNotifiedHealth;
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
     * THIS METHOD SHOULD ONLY BE USED FOR TESTING.
     * It allows to specify a custom validityPeriod, which by default has to be at least 60s.
     * With customizing the period, you can reduce the amount of waiting time for tests.
     */
    @VisibleForTesting
    void handleTimeoutHealthTesting(AgentHealth eventHealth, String loggerName, String eventMassage, Duration validityPeriod) {
        boolean isNotInfo = eventHealth.isMoreSevereOrEqualTo(AgentHealth.WARNING);

        if (isNotInfo) {
            generalHealthTimeouts.put(eventHealth, LocalDateTime.now().plus(validityPeriod));
        }
        String fullEventMessage = eventMassage + ". This status is valid for " + validityPeriod;
        triggerAgentHealthChangedEvent(loggerName, fullEventMessage, isNotInfo);
    }
}
