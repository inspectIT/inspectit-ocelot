package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

public interface HealthEventListener {

    @Async
    @EventListener
    void onAgentHealthEvent(AgentHealthChangedEvent event);

}
