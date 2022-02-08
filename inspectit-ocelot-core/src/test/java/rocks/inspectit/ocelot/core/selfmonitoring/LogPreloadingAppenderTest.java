package rocks.inspectit.ocelot.core.selfmonitoring;

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

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

/**
 * Tests {@link LogPreloadingAppender}
 */
@ExtendWith(MockitoExtension.class)
public class LogPreloadingAppenderTest {

    private LogPreloadingAppender logPreloadingAppender;

    @BeforeEach
    void resetLogPreloadingAppender() {
        try {
            Field field = LogPreloadingAppender.class.getDeclaredField("preloader");
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
            logPreloadingAppender.append(infoEvent);
            logPreloadingAppender.append(infoEvent);
            logPreloadingAppender.append(infoEvent);
            logPreloadingAppender.append(infoEvent);

            LogPreloader preloader = Mockito.mock(LogPreloader.class);
            logPreloadingAppender.registerPreloader(preloader);

            verify(preloader, times(0)).record(any());

            logPreloadingAppender.append(infoEvent);
            logPreloadingAppender.append(infoEvent);

            verify(preloader, times(2)).record(any());
            verifyNoMoreInteractions(preloader);
        }

        @Test
        void logInfoMessageAfterRegister() {

            ILoggingEvent infoEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogMetricsRecorderTest.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});

            LogPreloader preloader = Mockito.mock(LogPreloader.class);
            logPreloadingAppender.registerPreloader(preloader);

            verifyZeroInteractions(preloader);

            logPreloadingAppender.append(infoEvent);
            logPreloadingAppender.append(infoEvent);

            verify(preloader, times(2)).record(any());
            verifyNoMoreInteractions(preloader);
        }
    }
}