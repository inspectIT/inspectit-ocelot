package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.core.config.propertysources.http.HttpConfigurationPoller;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

public class PollerWritingHealthEventListener implements HealthEventListener {

    @Autowired
    HttpConfigurationPoller httpConfigurationPoller;

    @Override
    public void onAgentHealthEvent(AgentHealthChangedEvent event) {
        httpConfigurationPoller.updateAgentHealth(event.getNewHealth());
    }
}
