package rocks.inspectit.ocelot.core.selfmonitoring;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthIncident;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests {@link AgentHealthManager}
 */
@ExtendWith(MockitoExtension.class)
public class AgentHealthManagerTest {

    @InjectMocks
    private AgentHealthManager healthManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitEnvironment environment;
    @Mock
    private ScheduledExecutorService executor;
    @Mock
    private ApplicationContext ctx;
    @Mock
    private AgentHealthIncidentBuffer incidentBuffer;

    @Nested
    class InvalidatableHealth {

        @Test
        void verifyAgentHealthChangedEvent() {
            healthManager.notifyAgentHealth(AgentHealth.WARNING, this.getClass(), this.getClass().getName(), "Mock message");

            verify(ctx).publishEvent(any(AgentHealthChangedEvent.class));
        }

        @Test
        void verifyAgentHealthIncidentAddedEvent() {
            healthManager.notifyAgentHealth(AgentHealth.WARNING, this.getClass(), this.getClass().getName(), "Mock message");

            verify(incidentBuffer).put(any(AgentHealthIncident.class));
        }

        @Test
        void verifyNoAgentHealthIncidentAddedEvent() {
            healthManager.notifyAgentHealth(AgentHealth.OK, this.getClass(), this.getClass().getName(), "Mock message");

            verifyNoInteractions(incidentBuffer);
        }

        @Test
        void verifyInvalidateAgentHealthIncident() {
            healthManager.notifyAgentHealth(AgentHealth.ERROR, this.getClass(), this.getClass().getName(), "Mock message");
            healthManager.invalidateIncident(this.getClass(), "Mock invalidation");

            verify(ctx, times(2)).publishEvent(any(AgentHealthChangedEvent.class));
            verify(incidentBuffer, times(2)).put(any(AgentHealthIncident.class));
        }
    }
    @Nested
    class TimeoutHealth {

        @BeforeEach
        void setUpValidityPeriod() {
            when(environment.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod())
                    .thenReturn(Duration.ofSeconds(5));
        }

        @Test
        void verifyAgentHealthChangedEvent() {
            healthManager.notifyAgentHealth(AgentHealth.WARNING, null, this.getClass().getName(), "Mock message");

            verify(ctx).publishEvent(any(AgentHealthChangedEvent.class));
        }

        @Test
        void verifyAgentHealthIncidentAddedEvent() {
            healthManager.notifyAgentHealth(AgentHealth.WARNING, null, this.getClass().getName(), "Mock message");

            verify(incidentBuffer).put(any(AgentHealthIncident.class));
        }

        @Test
        void verifyNoAgentHealthIncidentAddedEvent() {
            healthManager.notifyAgentHealth(AgentHealth.OK, null, this.getClass().getName(), "Mock message");

            verifyNoInteractions(incidentBuffer);
        }

        @Test
        void verifyAgentHealthTimeout() throws InterruptedException {
            when(environment.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod())
                    .thenReturn(Duration.ofSeconds(5));

            healthManager.notifyAgentHealth(AgentHealth.ERROR, null, this.getClass().getName(), "Mock message");
            // Wait 6s for time out (= 5s validityPeriod + 1s buffer)
            Thread.sleep(6000);

            assertEquals(healthManager.getCurrentHealth(), AgentHealth.OK);
        }

        @Test
        void verifyCheckAgentHealth() throws InterruptedException {
            when(environment.getCurrentConfig().getSelfMonitoring().getAgentHealth().getMinHealthCheckDelay())
                    .thenReturn(Duration.ofSeconds(1));

            healthManager.notifyAgentHealth(AgentHealth.ERROR, null, this.getClass().getName(), "Mock message");
            // Wait 6s for time out (= 5s validityPeriod + 1s buffer)
            Thread.sleep(6000);

            healthManager.checkHealthAndSchedule();

            verify(ctx, times(2)).publishEvent(any(AgentHealthChangedEvent.class));
            verify(incidentBuffer, times(2)).put(any(AgentHealthIncident.class));
        }

        @Test
        void verifyCheckAgentHealthTooEarly() {
            when(environment.getCurrentConfig().getSelfMonitoring().getAgentHealth().getMinHealthCheckDelay())
                    .thenReturn(Duration.ofSeconds(1));

            healthManager.notifyAgentHealth(AgentHealth.ERROR, null, this.getClass().getName(), "Mock message");
            healthManager.checkHealthAndSchedule();

            verify(ctx, times(1)).publishEvent(any(AgentHealthChangedEvent.class));
            verify(incidentBuffer, times(2)).put(any(AgentHealthIncident.class));
        }
    }
}
