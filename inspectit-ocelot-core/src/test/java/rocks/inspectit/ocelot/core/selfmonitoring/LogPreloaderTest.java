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
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.selfmonitoring.LogPreloadingSettings;
import rocks.inspectit.ocelot.config.model.selfmonitoring.SelfMonitoringSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        InspectitEnvironment env = Mockito.mock(InspectitEnvironment.class);
        Mockito.when(env.getCurrentConfig()).thenReturn(createConfig(DEFAULT_BUFFER_SIZE));
        logPreloader = new LogPreloader(env);
    }

    private static InspectitConfig createConfig(int bufferSize) {
        LogPreloadingSettings logSettings = new LogPreloadingSettings();
        logSettings.setBufferSize(bufferSize);
        SelfMonitoringSettings smSettings = new SelfMonitoringSettings();
        smSettings.setLogPreloading(logSettings);
        InspectitConfig config = new InspectitConfig();
        config.setSelfMonitoring(smSettings);
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

            logPreloader.inspectitConfigurationChanged(createBufferChangedEvent(2 * DEFAULT_BUFFER_SIZE));
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs().spliterator(), false).count()).isZero();

            IntStream.range(0, DEFAULT_BUFFER_SIZE + 1).forEach(n -> logPreloader.record(warnEvent));
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs().spliterator(), false)
                    .count()).isEqualTo(DEFAULT_BUFFER_SIZE + 1);
            assertThat(StreamSupport.stream(logPreloader.getPreloadedLogs()
                    .spliterator(), false)).extracting(ILoggingEvent::getLevel).containsOnly(Level.WARN);
        }

        private InspectitConfigChangedEvent createBufferChangedEvent(int newBufferSize) {
            try {
                Constructor<InspectitConfigChangedEvent> constructor = InspectitConfigChangedEvent.class.getDeclaredConstructor(Object.class, InspectitConfig.class, InspectitConfig.class);
                constructor.setAccessible(true);
                return constructor.newInstance(this, null, createConfig(newBufferSize));
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

}