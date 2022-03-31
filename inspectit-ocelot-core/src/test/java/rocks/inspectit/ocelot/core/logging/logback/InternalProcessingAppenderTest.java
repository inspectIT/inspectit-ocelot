package rocks.inspectit.ocelot.core.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.core.instrumentation.InstrumentationManager;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.selfmonitoring.LogMetricsRecorderTest;

import java.lang.reflect.Field;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Tests {@link InternalProcessingAppender}
 */
@ExtendWith(MockitoExtension.class)
public class InternalProcessingAppenderTest {

    private InternalProcessingAppender appender;

    @BeforeEach
    void resetLogPreloadingAppender() {
        appender = new InternalProcessingAppender();

        try {
            Field field = InternalProcessingAppender.class.getDeclaredField("observers");
            field.setAccessible(true);
            ((Map<?, ?>) field.get(null)).clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Nested
    class Append {

        @Test
        void logGeneralMessages() {

            ILoggingEvent infoEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogMetricsRecorderTest.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});

            InternalProcessingAppender.Observer observer = Mockito.mock(InternalProcessingAppender.Observer.class);
            appender.register(observer);

            verifyNoInteractions(observer);

            appender.append(infoEvent);
            appender.append(infoEvent);

            verify(observer, times(2)).onLoggingEvent(any(), eq(null));
            verifyNoMoreInteractions(observer);
        }

        @Test
        void logInstrumentationMessages() {

            ILoggingEvent instrumentationEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(InstrumentationManager.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});

            InternalProcessingAppender.Observer observer = Mockito.mock(InternalProcessingAppender.Observer.class);
            appender.register(observer);

            verifyNoInteractions(observer);

            appender.append(instrumentationEvent);
            appender.append(instrumentationEvent);

            verify(observer, times(2)).onLoggingEvent(any(), eq(InstrumentationConfigurationChangedEvent.class));
            verifyNoMoreInteractions(observer);
        }

        @Test
        void logGeneralAndInstrumentationMessages() {

            ILoggingEvent generalEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogMetricsRecorderTest.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});
            ILoggingEvent instrumentationEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(InstrumentationManager.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});

            InternalProcessingAppender.Observer observer = Mockito.mock(InternalProcessingAppender.Observer.class);
            appender.register(observer);

            verifyNoInteractions(observer);

            appender.append(instrumentationEvent);
            appender.append(generalEvent);
            appender.append(generalEvent);
            appender.append(instrumentationEvent);

            verify(observer, times(2)).onLoggingEvent(any(), eq(null));
            verify(observer, times(2)).onLoggingEvent(any(), eq(InstrumentationConfigurationChangedEvent.class));
            verifyNoMoreInteractions(observer);
        }
    }
}