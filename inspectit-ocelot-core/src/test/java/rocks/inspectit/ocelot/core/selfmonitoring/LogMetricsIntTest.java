package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests the functionality of the {@link LogMetricsAppender} and {@link LogMetricsRecorder} against the {@link SelfMonitoringService}.
 */
@ExtendWith(MockitoExtension.class)
public class LogMetricsIntTest {

    @InjectMocks
    private LogMetricsRecorder recorder;

    @Mock
    private SelfMonitoringService monitoringService;

    private LogMetricsAppender appender;

    private LoggingEvent createLoggingEvent(Level level, String message) {
        Logger logger = LoggerFactory.getLogger(LogMetricsIntTest.class);
        return new LoggingEvent("fqcn", (ch.qos.logback.classic.Logger) logger, level, message, null, null);
    }

    @BeforeEach
    public void before() {
        LogMetricsAppender.registerRecorder(null);
        appender = new LogMetricsAppender();
    }

    @Test
    public void appendLogEvents() {
        // append log message without registered recorder
        appender.append(createLoggingEvent(Level.INFO, "info_test"));
        appender.append(createLoggingEvent(Level.INFO, "info_test"));
        appender.append(createLoggingEvent(Level.WARN, "warn_test"));

        verifyZeroInteractions(monitoringService);

        // register recorder and record existing metrics
        LogMetricsAppender.registerRecorder(recorder);

        verify(monitoringService).recordMeasurement(eq("logs"), eq(2L), eq(Collections.singletonMap("level", "INFO")));
        verify(monitoringService).recordMeasurement(eq("logs"), eq(1L), eq(Collections.singletonMap("level", "WARN")));
        verifyNoMoreInteractions(monitoringService);

        // append log message with registered recorder
        appender.append(createLoggingEvent(Level.ERROR, "error_test"));
        appender.append(createLoggingEvent(Level.ERROR, "error_test"));

        verify(monitoringService, times(2)).recordMeasurement(eq("logs"), eq(1L), eq(Collections.singletonMap("level", "ERROR")));
        verifyNoMoreInteractions(monitoringService);
    }
}
