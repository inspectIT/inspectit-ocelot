package rocks.inspectit.ocelot.core.selfmonitoring.logs;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.logging.logback.InternalProcessingAppender;
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogHealthMonitor  implements InternalProcessingAppender.LogEventConsumer {

    @Autowired
    AgentHealthManager agentHealthManager;

    private final ScheduledExecutorService executor;

    private final InspectitEnvironment env;

    private final SelfMonitoringService selfMonitoringService;

    @Override
    public void onLoggingEvent(ILoggingEvent event, Class<?> invalidator) {
        if (AgentHealthManager.class.getCanonicalName().equals(event.getLoggerName())) {
            // ignore own logs, which otherwise would tend to cause infinite loops
            return;
        }
        AgentHealth eventHealth = AgentHealth.fromLogLevel(event.getLevel());
        agentHealthManager.handleInvalidatableHealth(eventHealth, invalidator, "Logging health change"); //TODO We need a more speaking message here
    }

    @Override
    public void onInvalidationEvent(Object invalidator) {
        agentHealthManager.invalidateIncident(invalidator.getClass(), "Logging health change"); //TODO We need a more speaking message here
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
     * </ul>validityPeriod
     */
    private void checkHealthAndSchedule() {
        Duration delay = agentHealthManager.getNextHealthTimeoutDuration();

        executor.schedule(this::checkHealthAndSchedule, delay.toMillis(), TimeUnit.MILLISECONDS);
    }
}
