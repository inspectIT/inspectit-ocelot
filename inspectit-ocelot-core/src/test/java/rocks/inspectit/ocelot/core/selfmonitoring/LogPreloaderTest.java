package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.selfmonitoring.LogPreloadingSettings;

import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests @{@link LogPreloader}
 */
@ExtendWith(MockitoExtension.class)
public class LogPreloaderTest {

    private static final int DEFAULT_BUFFER_SIZE = 10;

    private LogPreloader logPreloader;

    @BeforeEach
    void setupPreloader() {
        logPreloader = new LogPreloader();
        logPreloader.doEnable(createConfig(DEFAULT_BUFFER_SIZE));
    }

    private static InspectitConfig createConfig(int bufferSize) {
        LogPreloadingSettings logSettings = new LogPreloadingSettings();
        logSettings.setBufferSize(bufferSize);
        InspectitConfig config = new InspectitConfig();
        config.setLogPreloading(logSettings);
        return config;
    }

    @Nested
    class Record {

        private ILoggingEvent infoEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogMetricsRecorderTest.class), Level.INFO, "Dummy Info", new Throwable(), new String[]{});

        private ILoggingEvent warnEvent = new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogMetricsRecorderTest.class), Level.WARN, "Dummy Info", new Throwable(), new String[]{});

        @Test
        void readWhenEmpty() {
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs().spliterator(), false).count()).isZero();
        }

        @Test
        void logOneInfoMessage() {
            logPreloader.record(infoEvent);
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs().spliterator(), false).count()).isOne();
        }

        @Test
        void logMultipleInfoMessages() {
            IntStream.range(0, DEFAULT_BUFFER_SIZE - 2).forEach(n -> logPreloader.record(infoEvent));
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs().spliterator(), false)
                    .count()).isEqualTo(DEFAULT_BUFFER_SIZE - 2);
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs()
                    .spliterator(), false)).extracting(ILoggingEvent::getLevel).containsOnly(Level.INFO);
        }

        @Test
        void logMoreMessagesThanBufferSize() {
            logPreloader.record(warnEvent);
            IntStream.range(0, DEFAULT_BUFFER_SIZE).forEach(n -> logPreloader.record(infoEvent));

            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs().spliterator(), false)
                    .count()).isEqualTo(DEFAULT_BUFFER_SIZE);
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs()
                    .spliterator(), false)).extracting(ILoggingEvent::getLevel).containsOnly(Level.INFO);
        }

        @Test
        void logMessagesAndChangeBufferSize() {
            IntStream.range(0, DEFAULT_BUFFER_SIZE / 2).forEach(n -> logPreloader.record(infoEvent));
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs().spliterator(), false)
                    .count()).isEqualTo(DEFAULT_BUFFER_SIZE / 2);

            logPreloader.doEnable(createConfig(2 * DEFAULT_BUFFER_SIZE));
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs().spliterator(), false).count()).isZero();

            IntStream.range(0, DEFAULT_BUFFER_SIZE + 1).forEach(n -> logPreloader.record(warnEvent));
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs().spliterator(), false)
                    .count()).isEqualTo(DEFAULT_BUFFER_SIZE + 1);
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs()
                    .spliterator(), false)).extracting(ILoggingEvent::getLevel).containsOnly(Level.WARN);
        }

    }

}