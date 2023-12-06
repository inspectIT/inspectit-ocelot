package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

public class LogWritingHealthEventListenerTest {

    private final LogWritingHealthEventListener logWritingHealthEventListener = new LogWritingHealthEventListener();

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        listAppender = new ListAppender<>();
        listAppender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(listAppender);
    }

    @Test
    void verifyAgentHealthLogging() {
        String eventMessage = "Mock message";
        AgentHealthChangedEvent event = new AgentHealthChangedEvent(this, AgentHealth.OK, AgentHealth.WARNING, eventMessage);
        String expectedFullMessage = String.format("The agent status changed from %s to %s. Reason: %s",
                AgentHealth.OK, AgentHealth.WARNING, eventMessage);

        logWritingHealthEventListener.onAgentHealthEvent(event);

        assertTrue(listAppender.list.stream().anyMatch(logEvent -> logEvent.getFormattedMessage().contains(expectedFullMessage)));
    }
}
