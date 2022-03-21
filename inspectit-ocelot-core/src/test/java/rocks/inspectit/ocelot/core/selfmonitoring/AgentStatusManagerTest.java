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
import rocks.inspectit.ocelot.commons.models.status.AgentStatus;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.selfmonitoring.AgentStatusSettings;
import rocks.inspectit.ocelot.config.model.selfmonitoring.SelfMonitoringSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.selfmonitoring.event.AgentStatusChangedEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Tests {@link AgentStatusManager}
 */
@ExtendWith(MockitoExtension.class)
public class AgentStatusManagerTest {

    private static final long VALIDITY_PERIOD_MILLIS = 500;

    private ScheduledExecutorService executorService;

    private InspectitConfig config;

    private InspectitEnvironment environment;

    private ApplicationContext context;

    private AgentStatusManager statusManager;

    @BeforeAll
    static void setupExecutorService() {

    }

    @BeforeEach
    void setupStatusManager() {
        executorService = new ScheduledThreadPoolExecutor(1);

        config = new InspectitConfig();
        AgentStatusSettings agentStatus = new AgentStatusSettings();
        agentStatus.setValidityPeriod(Duration.ofMillis(VALIDITY_PERIOD_MILLIS));
        SelfMonitoringSettings selfMonitoring = new SelfMonitoringSettings();
        selfMonitoring.setAgentStatus(agentStatus);
        config.setSelfMonitoring(selfMonitoring);

        environment = mock(InspectitEnvironment.class);
        when(environment.getCurrentConfig()).thenReturn(config);

        context = mock(ApplicationContext.class);

        statusManager = new AgentStatusManager(context, executorService, environment);

        try {
            Method startEventTriggerMethod = AgentStatusManager.class.getDeclaredMethod("startEventTrigger");
            startEventTriggerMethod.setAccessible(true);
            startEventTriggerMethod.invoke(statusManager);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    void shutdownExecutorService() {
        executorService.shutdown();
    }

    private ILoggingEvent createLoggingEvent(Level level) {
        return new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(AgentStatusManagerTest.class), level, "Dummy Info", new Throwable(), new String[]{});
    }

    private void verifyExactlyOneEventWasPublished(AgentStatus status) {
        ArgumentCaptor<AgentStatusChangedEvent> statusCaptor = ArgumentCaptor.forClass(AgentStatusChangedEvent.class);
        verify(context).publishEvent(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getNewStatus()).isEqualTo(status);
        verifyNoMoreInteractions(context);
    }

    private void resetInstrumentationStatus() {
        try {
            Method resetMethod = AgentStatusManager.class.getDeclaredMethod("resetInstrumentationStatus", InstrumentationConfigurationChangedEvent.class);
            resetMethod.setAccessible(true);
            resetMethod.invoke(statusManager, new InstrumentationConfigurationChangedEvent(this, null, null));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Nested
    class OnLogEvent {

        @Test
        void logInstrumentationEvents() {
            assertThat(statusManager.getCurrentStatus()).withFailMessage("Initial status shall be OK")
                    .isEqualTo(AgentStatus.OK);

            statusManager.onInstrumentationLoggingEvent(createLoggingEvent(Level.INFO));
            statusManager.onInstrumentationLoggingEvent(createLoggingEvent(Level.DEBUG));
            assertThat(statusManager.getCurrentStatus()).withFailMessage("INFO and DEBUG messages shall not change the status")
                    .isEqualTo(AgentStatus.OK);

            verifyNoInteractions(context);

            statusManager.onInstrumentationLoggingEvent(createLoggingEvent(Level.WARN));
            assertThat(statusManager.getCurrentStatus()).withFailMessage("Status after WARN message shall be WARNING")
                    .isEqualTo(AgentStatus.WARNING);
            verifyExactlyOneEventWasPublished(AgentStatus.WARNING);

            clearInvocations(context);

            statusManager.onInstrumentationLoggingEvent(createLoggingEvent(Level.ERROR));
            assertThat(statusManager.getCurrentStatus()).withFailMessage("Status after ERROR message shall be ERROR")
                    .isEqualTo(AgentStatus.ERROR);
            verifyExactlyOneEventWasPublished(AgentStatus.ERROR);

            clearInvocations(context);

            statusManager.onInstrumentationLoggingEvent(createLoggingEvent(Level.INFO));
            assertThat(statusManager.getCurrentStatus()).withFailMessage("INFO messages shall not change the status")
                    .isEqualTo(AgentStatus.ERROR);
            verifyNoMoreInteractions(context);

            clearInvocations(context);

            resetInstrumentationStatus();
            assertThat(statusManager.getCurrentStatus()).withFailMessage("When new instrumentation was triggered, status shall be OK")
                    .isEqualTo(AgentStatus.OK);
            verifyExactlyOneEventWasPublished(AgentStatus.OK);
        }

        @Test
        void logGeneralEvents() throws InterruptedException {
            assertThat(statusManager.getCurrentStatus()).withFailMessage("Initial status shall be OK")
                    .isEqualTo(AgentStatus.OK);

            statusManager.onGeneralLoggingEvent(createLoggingEvent(Level.INFO));
            statusManager.onGeneralLoggingEvent(createLoggingEvent(Level.DEBUG));
            assertThat(statusManager.getCurrentStatus()).withFailMessage("INFO and DEBUG messages shall not change the status")
                    .isEqualTo(AgentStatus.OK);

            verifyNoInteractions(context);

            statusManager.onGeneralLoggingEvent(createLoggingEvent(Level.ERROR));
            assertThat(statusManager.getCurrentStatus()).withFailMessage("Status after ERROR message shall be ERROR")
                    .isEqualTo(AgentStatus.ERROR);
            verifyExactlyOneEventWasPublished(AgentStatus.ERROR);

            clearInvocations(context);

            await().atMost(VALIDITY_PERIOD_MILLIS * 2, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> assertThat(statusManager.getCurrentStatus()).withFailMessage("ERROR status should jump back to OK after timeout")
                            .isEqualTo(AgentStatus.OK));

            await().atMost(VALIDITY_PERIOD_MILLIS * 2, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> verifyExactlyOneEventWasPublished(AgentStatus.OK));
        }

    }

}
