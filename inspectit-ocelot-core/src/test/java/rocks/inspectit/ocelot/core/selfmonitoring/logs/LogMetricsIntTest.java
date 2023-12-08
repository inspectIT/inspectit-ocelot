package rocks.inspectit.ocelot.core.selfmonitoring.logs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.core.logging.logback.InternalProcessingAppender;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests the functionality of the {@link InternalProcessingAppender} and {@link LogMetricsRecorder} against the {@link SelfMonitoringService}.
 */
@ExtendWith(MockitoExtension.class)
public class LogMetricsIntTest {

    @InjectMocks
    private LogMetricsRecorder recorder;

    @Mock
    private SelfMonitoringService monitoringService;

    private InternalProcessingAppender appender;

    private LoggingEvent createLoggingEvent(Level level, String message) {
        Logger logger = LoggerFactory.getLogger(LogMetricsIntTest.class);
        return new LoggingEvent("fqcn", (ch.qos.logback.classic.Logger) logger, level, message, null, null);
    }

    private void appendEvent(ILoggingEvent event) {
        Method appendMethod = null;
        try {
            appendMethod = InternalProcessingAppender.class.getDeclaredMethod("append", ILoggingEvent.class);
            appendMethod.setAccessible(true);
            appendMethod.invoke(appender, event);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @BeforeEach
    public void before() {
        try {
            Field field = InternalProcessingAppender.class.getDeclaredField("observers");
            field.setAccessible(true);
            ((Map<?, ?>) field.get(null)).clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        appender = new InternalProcessingAppender();
    }

    @Test
    public void appendLogEvents() {
        // append log message without registered recorder
        appendEvent(createLoggingEvent(Level.INFO, "info_test"));
        appendEvent(createLoggingEvent(Level.INFO, "info_test"));
        appendEvent(createLoggingEvent(Level.WARN, "warn_test"));

        verifyNoMoreInteractions(monitoringService);

        // register recorder (will not record previously appended events)
        InternalProcessingAppender.register(recorder);

        verifyNoMoreInteractions(monitoringService);

        // append log message with registered recorder
        appendEvent(createLoggingEvent(Level.ERROR, "error_test"));
        appendEvent(createLoggingEvent(Level.ERROR, "error_test"));

        verify(monitoringService, times(2)).recordMeasurement(eq("logs"), eq(1L), eq(Collections.singletonMap("level", "ERROR")));
        verifyNoMoreInteractions(monitoringService);
    }
}
