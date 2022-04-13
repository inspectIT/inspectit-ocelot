package rocks.inspectit.ocelot.core.selfmonitoring;

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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests @{@link LogMetricsRecorder}
 */
@ExtendWith(MockitoExtension.class)
public class LogMetricsRecorderTest {

    @Mock
    private SelfMonitoringService selfMonitoringService;

    @InjectMocks
    private LogMetricsRecorder logMetricsRecorder;

    @Nested
    class Increment {

        private ILoggingEvent infoEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogMetricsRecorderTest.class), ch.qos.logback.classic.Level.INFO, "Dummy Info", new Throwable(), new String[]{});

        @Test
        void incrementOneInfoMessage() {
            logMetricsRecorder.onLoggingEvent(infoEvent, null);
            verify(selfMonitoringService, times(1)).recordMeasurement(anyString(), eq(1L), anyMap());
            verifyNoMoreInteractions(selfMonitoringService);

        }

        @Test
        void incrementMultipleInfoMessages() {
            for (int i = 0; i < 4; i++) {
                logMetricsRecorder.onLoggingEvent(infoEvent, null);
                logMetricsRecorder.onLoggingEvent(infoEvent, null);
            }

            verify(selfMonitoringService, times(8)).recordMeasurement(anyString(), eq(1L), anyMap());

            verifyNoMoreInteractions(selfMonitoringService);
        }
    }
}