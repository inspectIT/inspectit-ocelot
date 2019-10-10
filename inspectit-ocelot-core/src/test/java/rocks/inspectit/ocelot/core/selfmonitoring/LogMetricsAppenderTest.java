package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Tests {@link LogMetricsAppender}
 */
@ExtendWith(MockitoExtension.class)
public class LogMetricsAppenderTest {

    @Mock
    private Map<String, Long> logCounts;

    @InjectMocks
    private LogMetricsAppender logMetricsAppender;

    @BeforeEach
    void resetLogMetricsAppender() {
        try {
            Field field = LogMetricsAppender.class.getDeclaredField("logMetricsRecorder");
            field.setAccessible(true);
            field.set(null, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    @Nested
    class Append {

        @Test
        void logInfoMessageAndRegister() {

            ILoggingEvent infoEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogMetricsRecorderTest.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});
            logMetricsAppender.append(infoEvent);
            logMetricsAppender.append(infoEvent);
            logMetricsAppender.append(infoEvent);
            logMetricsAppender.append(infoEvent);

            LogMetricsRecorder recorder = Mockito.mock(LogMetricsRecorder.class);
            logMetricsAppender.registerRecorder(recorder);

            verify(recorder, times(1)).increment("INFO", 4L);

            logMetricsAppender.append(infoEvent);
            logMetricsAppender.append(infoEvent);

            verify(recorder, times(2)).increment("INFO", 1L);
            verifyNoMoreInteractions(recorder);
        }

        @Test
        void logInfoMessageAfterRegister() {

            ILoggingEvent infoEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogMetricsRecorderTest.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});

            LogMetricsRecorder recorder = Mockito.mock(LogMetricsRecorder.class);
            logMetricsAppender.registerRecorder(recorder);

            verifyZeroInteractions(recorder);

            logMetricsAppender.append(infoEvent);
            logMetricsAppender.append(infoEvent);

            verify(recorder, times(2)).increment("INFO", 1L);
            verifyNoMoreInteractions(recorder);
        }
    }
}