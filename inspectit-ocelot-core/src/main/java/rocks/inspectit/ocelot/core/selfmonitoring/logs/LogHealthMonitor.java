package rocks.inspectit.ocelot.core.selfmonitoring.logs;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
public class LogHealthMonitor implements InternalProcessingAppender.LogEventConsumer {

    @Autowired
    AgentHealthManager agentHealthManager;

    public LogHealthMonitor(ApplicationContext applicationContext, ScheduledExecutorService scheduledExecutorService, InspectitEnvironment inspectitEnvironment, SelfMonitoringService selfMonitoringService) {
    }

    @Override
    public void onLoggingEvent(ILoggingEvent event, Class<?> invalidator) {
        if (AgentHealthManager.class.getCanonicalName().equals(event.getLoggerName())) {
            // ignore own logs, which otherwise would tend to cause infinite loops
            return;
        }
        AgentHealth eventHealth = AgentHealth.fromLogLevel(event.getLevel());
        agentHealthManager.handleInvalidatableHealth(eventHealth, invalidator, event.getMessage());
    }

    @Override
    public void onInvalidationEvent(Object invalidator) {
        agentHealthManager.invalidateIncident(invalidator.getClass(), "Invalidation due to invalidator event");
    }

    @PostConstruct
    @VisibleForTesting
    void registerAtAppender() {
        InternalProcessingAppender.register(this);
    }

    @PreDestroy
    @VisibleForTesting
    void unregisterFromAppender() {
        InternalProcessingAppender.unregister(this);
    }


}
