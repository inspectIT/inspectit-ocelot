package rocks.inspectit.ocelot.core.selfmonitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthIncident;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthIncidentAddedEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentHealthIncidentBufferTest {

    @InjectMocks
    private AgentHealthIncidentBuffer incidentBuffer;
    @Mock
    private ApplicationContext ctx;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitEnvironment env;

    private AgentHealthIncident incident;

    @BeforeEach
    void setUp() {
        when(env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getIncidentBufferSize()).thenReturn(2);
        incident = new AgentHealthIncident("2001-01-01", AgentHealth.WARNING, this.getClass().getCanonicalName(), "Mock message", true);
    }
    @Test
    void verifyBufferSize() {
        incidentBuffer.put(incident);
        incidentBuffer.put(incident);
        incidentBuffer.put(incident);

        verify(ctx, times(3)).publishEvent(any(AgentHealthIncidentAddedEvent.class));
    }

    @Test
    void verifyEventPublisher() {
        incidentBuffer.put(incident);
        incidentBuffer.put(incident);
        incidentBuffer.put(incident);

        verify(ctx, times(3)).publishEvent(any(AgentHealthIncidentAddedEvent.class));
    }
}
