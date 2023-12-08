package rocks.inspectit.ocelot.core.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.core.instrumentation.InstrumentationManager;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.selfmonitoring.LogMetricsRecorderTest;

import static org.mockito.Mockito.*;

/**
 * Tests {@link InternalProcessingAppender}
 */
@ExtendWith(MockitoExtension.class)
public class InternalProcessingAppenderTest {

    private static final ILoggingEvent GENERAL_EVENT = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogMetricsRecorderTest.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});

    private static final ILoggingEvent INSTRUMENTATION_EVENT = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(InstrumentationManager.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});

    private static InternalProcessingAppender.LogEventConsumer logEventConsumer;

    private InternalProcessingAppender appender;

    @BeforeAll
    static void setupLogEventConsumer() {
        logEventConsumer = Mockito.mock(InternalProcessingAppender.LogEventConsumer.class);
        InternalProcessingAppender.register(logEventConsumer);
    }

    @BeforeEach
    void resetLogPreloadingAppender() {
        appender = new InternalProcessingAppender();
        reset(logEventConsumer);
    }

    @AfterAll
    static void unregisterLogEventConsumer() {
        InternalProcessingAppender.unregister(logEventConsumer);
    }

    @Nested
    class Append {

        @Test
        void logGeneralMessages() {
            verifyNoInteractions(logEventConsumer);

            appender.append(GENERAL_EVENT);
            appender.append(GENERAL_EVENT);

            verify(logEventConsumer, times(2)).onLoggingEvent(any(), eq(null));
            verifyNoMoreInteractions(logEventConsumer);
        }

        @Test
        void logInstrumentationMessages() {
            verifyNoInteractions(logEventConsumer);

            appender.append(INSTRUMENTATION_EVENT);
            appender.append(INSTRUMENTATION_EVENT);

            verify(logEventConsumer, times(2)).onLoggingEvent(any(), eq(InstrumentationConfigurationChangedEvent.class));
            verifyNoMoreInteractions(logEventConsumer);
        }

        @Test
        void logGeneralAndInstrumentationMessages() {
            verifyNoInteractions(logEventConsumer);

            appender.append(INSTRUMENTATION_EVENT);
            appender.append(GENERAL_EVENT);
            appender.append(GENERAL_EVENT);
            appender.append(INSTRUMENTATION_EVENT);

            verify(logEventConsumer, times(2)).onLoggingEvent(any(), eq(null));
            verify(logEventConsumer, times(2)).onLoggingEvent(any(), eq(InstrumentationConfigurationChangedEvent.class));
            verifyNoMoreInteractions(logEventConsumer);
        }
    }
}