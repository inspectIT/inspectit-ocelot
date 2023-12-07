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
 * Tests for {@link AgentHealthManager}
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
        void verifyAgentHealthChangedEventAfterInvalidatableHealth() {
            healthManager.handleInvalidatableHealth(AgentHealth.WARNING, this.getClass(), "Mock message");

            verify(ctx).publishEvent(any(AgentHealthChangedEvent.class));
        }

        @Test
        void verifyAgentHealthIncidentAddedEventAfterInvalidatableHealth() {
            healthManager.handleInvalidatableHealth(AgentHealth.WARNING, this.getClass(), "Mock message");

            verify(incidentBuffer).put(any(AgentHealthIncident.class));
        }

        @Test
        void verifyNoAgentHealthIncidentAddedEventAfterInvalidatableHealth() {
            healthManager.handleInvalidatableHealth(AgentHealth.OK, this.getClass(), "Mock message");

            verifyNoInteractions(incidentBuffer);
        }

        @Test
        void verifyAgentHealthIncidentInvalidation() {
            healthManager.handleInvalidatableHealth(AgentHealth.ERROR, this.getClass(), "Mock message");
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
        void verifyAgentHealthChangedEventAfterTimeoutHealth() {
            healthManager.handleTimeoutHealth(AgentHealth.WARNING, this.getClass().getName(), "Mock message");

            verify(ctx).publishEvent(any(AgentHealthChangedEvent.class));
        }

        @Test
        void verifyAgentHealthIncidentAddedEventAfterTimeoutHealth() {
            healthManager.handleTimeoutHealth(AgentHealth.WARNING, this.getClass().getName(), "Mock message");

            verify(incidentBuffer).put(any(AgentHealthIncident.class));
        }

        @Test
        void verifyNoAgentHealthIncidentAddedEventAfterTimeoutHealth() {
            healthManager.handleTimeoutHealth(AgentHealth.OK, this.getClass().getName(), "Mock message");;

            verifyNoInteractions(incidentBuffer);
        }

        @Test
        void verifyAgentHealthTimeout() throws InterruptedException {
            when(environment.getCurrentConfig().getSelfMonitoring().getAgentHealth().getValidityPeriod())
                    .thenReturn(Duration.ofSeconds(5));

            healthManager.handleTimeoutHealth(AgentHealth.OK, this.getClass().getName(), "Mock message");

            // Wait 6s for time out (= 5s validityPeriod + 1s buffer)
            Thread.sleep(6000);

            assertEquals(healthManager.getCurrentHealth(), AgentHealth.OK);
        }

        @Test
        void verifyCheckAgentHealth() throws InterruptedException {
            when(environment.getCurrentConfig().getSelfMonitoring().getAgentHealth().getMinHealthCheckDelay())
                    .thenReturn(Duration.ofSeconds(1));

            healthManager.handleTimeoutHealth(AgentHealth.ERROR, this.getClass().getName(), "Mock message");

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

            healthManager.handleTimeoutHealth(AgentHealth.ERROR, this.getClass().getName(), "Mock message");

            healthManager.checkHealthAndSchedule();

            verify(ctx, times(1)).publishEvent(any(AgentHealthChangedEvent.class));
            verify(incidentBuffer, times(2)).put(any(AgentHealthIncident.class));
        }
    }
}
