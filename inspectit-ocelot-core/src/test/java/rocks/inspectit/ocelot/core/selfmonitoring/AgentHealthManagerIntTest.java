package rocks.inspectit.ocelot.core.selfmonitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.propertysources.http.HttpConfigurationPoller;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests {@link AgentHealthManager}
 */
public class AgentHealthManagerIntTest extends SpringTestBase {

    @Autowired
    private AgentHealthManager healthManager;
    @Autowired
    private HttpConfigurationPoller configurationPoller;
    @Autowired
    private AgentHealthIncidentBuffer incidentBuffer;

    @BeforeEach
    void clearBuffer() {
        incidentBuffer.clear();
    }

    @Nested
    class InvalidatableHealth {

        @Test
        void verifyAgentHealthUpdating() {
            AgentHealth currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            int bufferSize = healthManager.getIncidentHistory().size();
            assertEquals(currentHealth, AgentHealth.OK);
            assertEquals(bufferSize, 0);

            healthManager.notifyAgentHealth(AgentHealth.WARNING, this.getClass(), this.getClass().getName(), "Mock message");

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            bufferSize = healthManager.getIncidentHistory().size();
            assertEquals(currentHealth, AgentHealth.WARNING);
            assertEquals(bufferSize, 1);

            healthManager.notifyAgentHealth(AgentHealth.ERROR, this.getClass(), this.getClass().getName(), "Mock message");

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            bufferSize = healthManager.getIncidentHistory().size();
            assertEquals(currentHealth, AgentHealth.ERROR);
            assertEquals(bufferSize, 2);
        }

        @Test
        void verifyAgentHealthInvalidation() {
            AgentHealth currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            int bufferSize = healthManager.getIncidentHistory().size();
            assertEquals(currentHealth, AgentHealth.OK);
            assertEquals(bufferSize, 0);

            healthManager.notifyAgentHealth(AgentHealth.ERROR, this.getClass(), this.getClass().getName(), "Mock message");

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            bufferSize = healthManager.getIncidentHistory().size();
            assertEquals(currentHealth, AgentHealth.ERROR);
            assertEquals(bufferSize, 1);

            healthManager.invalidateIncident(this.getClass(), "Mock invalidation");

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            bufferSize = healthManager.getIncidentHistory().size();
            assertEquals(currentHealth, AgentHealth.OK);
            assertEquals(bufferSize, 2);
        }
    }

    @Nested
    class TimeoutHealth {

        @Test
        void verifyAgentHealthUpdating() throws InterruptedException {
            AgentHealth currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            int bufferSize = healthManager.getIncidentHistory().size();
            assertEquals(currentHealth, AgentHealth.OK);
            assertEquals(bufferSize, 0);

            // Use custom method for testing, to reduce the validityPeriod
            // This method should not be used outside of tests!
            healthManager.handleTimeoutHealthTesting(AgentHealth.WARNING, this.getClass().getName(),
                    "Mock message", Duration.ofSeconds(10));

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            bufferSize = healthManager.getIncidentHistory().size();
            assertEquals(currentHealth, AgentHealth.WARNING);
            assertEquals(bufferSize, 1);

            // wait 61s for time out (which has to be at least 60s)
            Thread.sleep(61000);

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            bufferSize = healthManager.getIncidentHistory().size();
            assertEquals(currentHealth, AgentHealth.OK);
            assertEquals(bufferSize, 3);
        }
    }
}
