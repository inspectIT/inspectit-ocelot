package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.selfmonitoring.AgentHealthSettings;
import rocks.inspectit.ocelot.config.model.selfmonitoring.SelfMonitoringSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.selfmonitoring.event.AgentHealthChangedEvent;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Tests {@link AgentHealthManager}
 */
@ExtendWith(MockitoExtension.class)
public class AgentHealthManagerTest {

    private static final long VALIDITY_PERIOD_MILLIS = 500;

    private static InspectitConfig config;

    private ScheduledExecutorService executorService;

    private InspectitEnvironment environment;

    private ApplicationContext context;

    private AgentHealthManager healthManager;

    @BeforeAll
    static void createInspectitConfig() {
        config = new InspectitConfig();
        AgentHealthSettings agentHealth = new AgentHealthSettings();
        agentHealth.setValidityPeriod(Duration.ofMillis(VALIDITY_PERIOD_MILLIS));
        agentHealth.setMinHealthCheckDelay(Duration.ofMillis(0));
        SelfMonitoringSettings selfMonitoring = new SelfMonitoringSettings();
        selfMonitoring.setAgentHealth(agentHealth);
        config.setSelfMonitoring(selfMonitoring);
    }

    @BeforeEach
    void setupStatusManager() {

        executorService = new ScheduledThreadPoolExecutor(1);

        environment = mock(InspectitEnvironment.class);
        when(environment.getCurrentConfig()).thenReturn(config);

        context = mock(ApplicationContext.class);

        healthManager = new AgentHealthManager(context, executorService, environment, mock(SelfMonitoringService.class));
        healthManager.startHealthCheckScheduler();
    }

    @AfterEach
    void shutdownExecutorService() {
        executorService.shutdown();
    }

    private ILoggingEvent createLoggingEvent(Level level) {
        return new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(AgentHealthManagerTest.class), level, "Dummy Info", new Throwable(), new String[]{});
    }

    private void verifyExactlyOneEventWasPublished(AgentHealth status) {
        ArgumentCaptor<AgentHealthChangedEvent> statusCaptor = ArgumentCaptor.forClass(AgentHealthChangedEvent.class);
        verify(context).publishEvent(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getNewHealth()).isEqualTo(status);
        verifyNoMoreInteractions(context);
    }

    @Nested
    class OnLogEvent {

        @Test
        void logInstrumentationEvents() {
            assertThat(healthManager.getCurrentHealth()).withFailMessage("Initial status shall be OK")
                    .isEqualTo(AgentHealth.OK);

            healthManager.onLoggingEvent(createLoggingEvent(Level.INFO), InstrumentationConfigurationChangedEvent.class);
            healthManager.onLoggingEvent(createLoggingEvent(Level.DEBUG), InstrumentationConfigurationChangedEvent.class);
            assertThat(healthManager.getCurrentHealth()).withFailMessage("INFO and DEBUG messages shall not change the status")
                    .isEqualTo(AgentHealth.OK);

            verifyNoInteractions(context);

            healthManager.onLoggingEvent(createLoggingEvent(Level.WARN), InstrumentationConfigurationChangedEvent.class);
            assertThat(healthManager.getCurrentHealth()).withFailMessage("Status after WARN message shall be WARNING")
                    .isEqualTo(AgentHealth.WARNING);
            verifyExactlyOneEventWasPublished(AgentHealth.WARNING);

            clearInvocations(context);

            healthManager.onLoggingEvent(createLoggingEvent(Level.ERROR), InstrumentationConfigurationChangedEvent.class);
            assertThat(healthManager.getCurrentHealth()).withFailMessage("Status after ERROR message shall be ERROR")
                    .isEqualTo(AgentHealth.ERROR);
            verifyExactlyOneEventWasPublished(AgentHealth.ERROR);

            clearInvocations(context);

            healthManager.onLoggingEvent(createLoggingEvent(Level.INFO), InstrumentationConfigurationChangedEvent.class);
            assertThat(healthManager.getCurrentHealth()).withFailMessage("INFO messages shall not change the status")
                    .isEqualTo(AgentHealth.ERROR);
            verifyNoMoreInteractions(context);

            clearInvocations(context);

            healthManager.onInvalidationEvent(new InstrumentationConfigurationChangedEvent(this, null, null));
            assertThat(healthManager.getCurrentHealth()).withFailMessage("When new instrumentation was triggered, status shall be OK")
                    .isEqualTo(AgentHealth.OK);
            verifyExactlyOneEventWasPublished(AgentHealth.OK);
        }

        @Test
        void logGeneralEvents() {
            assertThat(healthManager.getCurrentHealth()).withFailMessage("Initial status shall be OK")
                    .isEqualTo(AgentHealth.OK);

            healthManager.onLoggingEvent(createLoggingEvent(Level.INFO), null);
            healthManager.onLoggingEvent(createLoggingEvent(Level.DEBUG), null);
            assertThat(healthManager.getCurrentHealth()).withFailMessage("INFO and DEBUG messages shall not change the status")
                    .isEqualTo(AgentHealth.OK);

            verifyNoInteractions(context);

            healthManager.onLoggingEvent(createLoggingEvent(Level.ERROR), null);
            assertThat(healthManager.getCurrentHealth()).withFailMessage("Status after ERROR message shall be ERROR")
                    .isEqualTo(AgentHealth.ERROR);
            verifyExactlyOneEventWasPublished(AgentHealth.ERROR);

            clearInvocations(context);

            await().atMost(VALIDITY_PERIOD_MILLIS * 2, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> assertThat(healthManager.getCurrentHealth()).withFailMessage("ERROR status should jump back to OK after timeout")
                            .isEqualTo(AgentHealth.OK));

            await().atMost(VALIDITY_PERIOD_MILLIS * 2, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> verifyExactlyOneEventWasPublished(AgentHealth.OK));
        }

    }

}
