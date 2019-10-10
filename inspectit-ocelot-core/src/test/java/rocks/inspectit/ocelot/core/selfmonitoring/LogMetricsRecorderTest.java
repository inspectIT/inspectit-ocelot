package rocks.inspectit.ocelot.core.selfmonitoring;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Level;

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

        @Test
        void incrementOneInfoMessage() {
            logMetricsRecorder.increment(Level.INFO.getName(), 1);
            verify(selfMonitoringService, times(1)).recordMeasurement(anyString(), eq(1L), anyMap());
            verifyNoMoreInteractions(selfMonitoringService);

        }

        @Test
        void incrementMultipleInfoMessages() {
            logMetricsRecorder.increment(Level.INFO.getName(), 8);
            verify(selfMonitoringService, times(1)).recordMeasurement(anyString(), eq(8L), anyMap());
            verifyNoMoreInteractions(selfMonitoringService);
        }
    }
}