package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthIncident;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthState;
import rocks.inspectit.ocelot.core.config.propertysources.http.HttpConfigurationPoller;
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthIncidentAddedEvent;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PollerWritingHealthEventListenerTest {

    @InjectMocks
    private PollerWritingHealthEventListener pollerWritingHealthEventListener;
    @Mock
    private HttpConfigurationPoller httpConfigurationPoller;
    @Mock
    private AgentHealthManager agentHealthManager;

    private List<AgentHealthIncident> lastIncidents;

    @BeforeEach
    void setUpIncidents() {
        AgentHealthIncident incident = new AgentHealthIncident("2000-01-01", AgentHealth.WARNING, this.getClass().getCanonicalName(), "Mock message", true);
        lastIncidents = Collections.singletonList(incident);
    }

    @Test
    void verifyAgentHealthUpdateOnChangedHealth() {
        when(agentHealthManager.getIncidentHistory()).thenReturn(lastIncidents);
        AgentHealthChangedEvent event = new AgentHealthChangedEvent(this, AgentHealth.OK, AgentHealth.WARNING, "Mock message");

        pollerWritingHealthEventListener.onAgentHealthEvent(event);
        AgentHealthState healthState = new AgentHealthState(AgentHealth.WARNING, this.toString(), "Mock message", lastIncidents);

        verify(httpConfigurationPoller).updateAgentHealthState(healthState);
    }
    @Test
    void verifyAgentHealthUpdateOnAddedIncident() {
        AgentHealthIncidentAddedEvent event = new AgentHealthIncidentAddedEvent(this, lastIncidents);

        pollerWritingHealthEventListener.onAgentHealthIncidentEvent(event);
        AgentHealthState healthState = new AgentHealthState(AgentHealth.WARNING, this.getClass().getCanonicalName(), "Mock message", lastIncidents);

        verify(httpConfigurationPoller).updateAgentHealthState(healthState);
    }
}
