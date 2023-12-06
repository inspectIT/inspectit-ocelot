package rocks.inspectit.ocelot.core.selfmonitoring.logs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.config.PropertySourcesReloadEvent;
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class LogHealthMonitorTest {

    @InjectMocks
    private LogHealthMonitor healthMonitor;
    @Mock
    private AgentHealthManager healthManager;

    private ILoggingEvent createLoggingEvent(Class<?> loggedClass, Level logLevel) {
        return new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(loggedClass), logLevel, "Dummy Info", new Throwable(), new String[]{});
    }

    @Test
    void ignoreLogsFromAgentHealthManagerClass() {
        ILoggingEvent loggingEvent = createLoggingEvent(AgentHealthManager.class, Level.INFO);

        healthMonitor.onLoggingEvent(loggingEvent, null);

        verifyNoMoreInteractions(healthManager);
    }

    @Test
    void verifyNotifyAgentHealthOnInfo() {
        Class<?> loggerClass = LogHealthMonitor.class;
        ILoggingEvent loggingEvent = createLoggingEvent(loggerClass, Level.INFO);
        Class<?> invalidatorMock = PropertySourcesReloadEvent.class;
        AgentHealth eventHealth = AgentHealth.OK;

        healthMonitor.onLoggingEvent(loggingEvent, invalidatorMock);

        verify(healthManager).notifyAgentHealth(eventHealth, invalidatorMock, loggerClass.getCanonicalName(), loggingEvent.getFormattedMessage());
    }

    @Test
    void verifyNotifyAgentHealthOnWarn() {
        Class<?> loggerClass = LogHealthMonitor.class;
        ILoggingEvent loggingEvent = createLoggingEvent(loggerClass, Level.WARN);
        Class<?> invalidatorMock = PropertySourcesReloadEvent.class;
        AgentHealth eventHealth = AgentHealth.WARNING;

        healthMonitor.onLoggingEvent(loggingEvent, invalidatorMock);

        verify(healthManager).notifyAgentHealth(eventHealth, invalidatorMock, loggerClass.getCanonicalName(), loggingEvent.getFormattedMessage());
    }

    @Test
    void verifyNotifyAgentHealthOnError() {
        Class<?> loggerClass = LogHealthMonitor.class;
        ILoggingEvent loggingEvent = createLoggingEvent(loggerClass, Level.ERROR);
        Class<?> invalidatorMock = PropertySourcesReloadEvent.class;
        AgentHealth eventHealth = AgentHealth.ERROR;

        healthMonitor.onLoggingEvent(loggingEvent, invalidatorMock);

        verify(healthManager).notifyAgentHealth(eventHealth, invalidatorMock, loggerClass.getCanonicalName(), loggingEvent.getFormattedMessage());
    }

    @Test
    void verifyNotifyAgentHealthOnTrace() {
        Class<?> loggerClass = LogHealthMonitor.class;
        ILoggingEvent loggingEvent = createLoggingEvent(loggerClass, Level.TRACE);
        Class<?> invalidatorMock = PropertySourcesReloadEvent.class;
        AgentHealth eventHealth = AgentHealth.OK;

        healthMonitor.onLoggingEvent(loggingEvent, invalidatorMock);

        verify(healthManager).notifyAgentHealth(eventHealth, invalidatorMock, loggerClass.getCanonicalName(), loggingEvent.getFormattedMessage());
    }

    @Test
    void verifyNotifyAgentHealthOnDebug() {
        Class<?> loggerClass = LogHealthMonitor.class;
        ILoggingEvent loggingEvent = createLoggingEvent(loggerClass, Level.DEBUG);
        Class<?> invalidatorMock = PropertySourcesReloadEvent.class;
        AgentHealth eventHealth = AgentHealth.OK;

        healthMonitor.onLoggingEvent(loggingEvent, invalidatorMock);

        verify(healthManager).notifyAgentHealth(eventHealth, invalidatorMock, loggerClass.getCanonicalName(), loggingEvent.getFormattedMessage());
    }
}
