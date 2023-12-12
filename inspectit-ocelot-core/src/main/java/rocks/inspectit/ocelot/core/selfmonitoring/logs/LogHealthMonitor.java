package rocks.inspectit.ocelot.core.selfmonitoring.logs;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.logging.logback.InternalProcessingAppender;
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogHealthMonitor implements InternalProcessingAppender.LogEventConsumer {

    @Autowired
    final AgentHealthManager agentHealthManager;

    @Override
    public void onLoggingEvent(ILoggingEvent event, Class<?> invalidator) {
        if (AgentHealthManager.class.getCanonicalName().equals(event.getLoggerName())) {
            // ignore own logs, which otherwise would tend to cause infinite loops
            return;
        }
        AgentHealth eventHealth = AgentHealth.fromLogLevel(event.getLevel());
        if(invalidator != null)
            agentHealthManager.handleInvalidatableHealth(eventHealth, invalidator, event.getFormattedMessage());
        else
            agentHealthManager.handleTimeoutHealth(eventHealth, event.getLoggerName(), event.getFormattedMessage());
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
