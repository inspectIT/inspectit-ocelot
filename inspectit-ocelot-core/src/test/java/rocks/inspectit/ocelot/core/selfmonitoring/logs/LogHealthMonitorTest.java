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

/**
 * Tests {@link AgentHealthManager}
 */
@ExtendWith(MockitoExtension.class)
public class LogHealthMonitorTest {

    @InjectMocks
    private LogHealthMonitor healthMonitor;

    @Mock
    private AgentHealthManager healthManager;

    private ILoggingEvent createLoggingEvent(Class<?> loggedClass) {
        return new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(loggedClass), Level.INFO, "Dummy Info", new Throwable(), new String[]{});
    }
    //TODO testen mit allen Levels (WARN, ERROR, OK, INFO, DEBUG)
    @Nested
    class OnLoggingEvent {

        @Test
        void ignoreLogsFromAgentHealthManagerClass() {
            ILoggingEvent loggingEvent = createLoggingEvent(AgentHealthManager.class);

            healthMonitor.onLoggingEvent(loggingEvent, null);

            verifyNoMoreInteractions(healthManager);
        }

        @Test
        void triggerAgentHealthManagerNotifyAgentHealthEvent() {
            Class<?> loggerClass = LogHealthMonitor.class;
            ILoggingEvent loggingEvent = createLoggingEvent(loggerClass);
            Class<?> invalidatorMock = PropertySourcesReloadEvent.class;
            AgentHealth eventHealth = AgentHealth.fromLogLevel(loggingEvent.getLevel());

            healthMonitor.onLoggingEvent(loggingEvent, invalidatorMock);

            verify(healthManager).notifyAgentHealth(eventHealth, invalidatorMock, loggerClass.getCanonicalName(), loggingEvent.getMessage());
        }
    }
}
