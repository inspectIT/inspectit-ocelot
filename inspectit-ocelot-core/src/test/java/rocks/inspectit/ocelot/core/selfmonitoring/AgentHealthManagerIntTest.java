package rocks.inspectit.ocelot.core.selfmonitoring;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.propertysources.http.HttpConfigurationPoller;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for {@link AgentHealthManager}
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestPropertySource(properties = "inspectit.self-monitoring.agent-health.validity-period:5s")
public class AgentHealthManagerIntTest extends SpringTestBase {

    @Autowired
    private AgentHealthManager healthManager;
    @Autowired
    private HttpConfigurationPoller configurationPoller;

    /**
     * Period how long a TimeOut AgentHealth is valid (+1s buffer)
     */
    private final long validityPeriod = 6000;

    @Nested
    class InvalidatableHealth {

        @Test
        void verifyAgentHealthUpdating() {
            AgentHealth currentManagerHealth = healthManager.getCurrentHealth();
            AgentHealth currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            assertEquals(currentHealth, currentManagerHealth);
            assertEquals(currentHealth, AgentHealth.OK);

            healthManager.handleInvalidatableHealth(AgentHealth.WARNING, this.getClass(), "Mock message");

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            currentManagerHealth = healthManager.getCurrentHealth();
            assertEquals(currentHealth, currentManagerHealth);
            assertEquals(currentHealth, AgentHealth.WARNING);

            healthManager.handleInvalidatableHealth(AgentHealth.ERROR, this.getClass(), "Mock message");

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            currentManagerHealth = healthManager.getCurrentHealth();
            assertEquals(currentHealth, currentManagerHealth);
            assertEquals(currentHealth, AgentHealth.ERROR);
        }

        @Test
        void verifyAgentHealthInvalidation() throws InterruptedException {
            AgentHealth currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            AgentHealth currentManagerHealth = healthManager.getCurrentHealth();
            assertEquals(currentHealth, currentManagerHealth);
            assertEquals(currentHealth, AgentHealth.OK);

            healthManager.handleInvalidatableHealth(AgentHealth.ERROR, this.getClass(), "Mock message");

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            currentManagerHealth = healthManager.getCurrentHealth();
            assertEquals(currentHealth, currentManagerHealth);
            assertEquals(currentHealth, AgentHealth.ERROR);

            healthManager.invalidateIncident(this.getClass(), "Mock invalidation");

            // simulate scheduler
            Thread.sleep(validityPeriod);
            healthManager.checkHealthAndSchedule();

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            currentManagerHealth = healthManager.getCurrentHealth();
            assertEquals(currentHealth, currentManagerHealth);
            assertEquals(currentHealth, AgentHealth.OK);
        }
    }

    @Nested
    class TimeoutHealth {

        @Test
        void verifyAgentHealthTimedOut() throws InterruptedException {
            AgentHealth currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            AgentHealth currentManagerHealth = healthManager.getCurrentHealth();
            assertEquals(currentHealth, currentManagerHealth);
            assertEquals(currentHealth, AgentHealth.OK);

            healthManager.handleTimeoutHealth(AgentHealth.WARNING, this.getClass().getName(), "Mock message");

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            currentManagerHealth = healthManager.getCurrentHealth();
            assertEquals(currentHealth, currentManagerHealth);
            assertEquals(currentHealth, AgentHealth.WARNING);

            // simulate scheduler
            Thread.sleep(validityPeriod);
            healthManager.checkHealthAndSchedule();

            currentHealth = configurationPoller.getCurrentAgentHealthState().getHealth();
            currentManagerHealth = healthManager.getCurrentHealth();
            assertEquals(currentHealth, currentManagerHealth);
            assertEquals(currentHealth, AgentHealth.OK);
        }
    }
}
