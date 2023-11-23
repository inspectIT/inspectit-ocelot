package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthIncident;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthState;
import rocks.inspectit.ocelot.core.config.propertysources.http.HttpConfigurationPoller;
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

import java.util.List;

@Component
public class PollerWritingHealthEventListener implements HealthEventListener {

    @Autowired
    HttpConfigurationPoller httpConfigurationPoller;

    @Autowired
    AgentHealthManager agentHealthManager;

    @Override
    public void onAgentHealthEvent(AgentHealthChangedEvent event) {
        List<AgentHealthIncident> incidentHistory = agentHealthManager.getHistory();
        AgentHealthState state = new AgentHealthState(event.getNewHealth(), event.getSource().toString(), event.getMessage(), incidentHistory);
        httpConfigurationPoller.updateAgentHealthState(state);
    }
}
