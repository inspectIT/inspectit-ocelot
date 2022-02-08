package rocks.inspectit.ocelot.core.command.handler.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand;
import rocks.inspectit.ocelot.config.model.selfmonitoring.LogPreloadingSettings;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.selfmonitoring.LogPreloader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
public class LogsCommandExecutorTest {

    static final String tmpDir = "tmpconf_int_test";

    static final String configurationA = "inspectit:\n  self-monitoring:\n    log-preloading:\n      enabled: true\n      log-level: WARN\n      buffer-size: 512";

    static final String configurationB = "inspectit:\n  self-monitoring:\n    log-preloading:\n      enabled: true\n      log-level: ERROR\n      buffer-size: 1024";

    @BeforeAll
    static void createConfigDir() {
        new File(tmpDir).mkdir();
    }

    @BeforeEach
    void cleanConfigDir() throws Exception {
        FileUtils.cleanDirectory(new File(tmpDir));
    }

    @AfterAll
    static void deleteConfigDir() throws Exception {
        FileUtils.deleteDirectory(new File(tmpDir));
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {"inspectit.config.file-based.path=" + tmpDir, "inspectit.config.file-based.frequency=50ms"})
    public class Configuration extends SpringTestBase {

        @Autowired
        InspectitEnvironment env;

        @Test
        public void test() throws IOException {
            testConfiguration(configurationA, true, Level.WARN, 512);
            testConfiguration(configurationB, true, Level.ERROR, 1024);
        }

        private void testConfiguration(String configuration, boolean enabled, Level logLevel, int bufferSize) throws IOException {
            FileUtils.write(new File(tmpDir + "/logs-command.yml"), configuration, Charset.defaultCharset());

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
                LogPreloadingSettings settings = env.getCurrentConfig().getSelfMonitoring().getLogPreloading();
                assertThat(settings).isNotNull();
                if (settings != null) {
                    assertThat(settings.isEnabled()).isEqualTo(enabled);
                    assertThat(settings.getLogLevel()).isEqualTo(logLevel);
                    assertThat(settings.getBufferSize()).isEqualTo(bufferSize);
                }
            });
        }

    }

    @Nested
    @DirtiesContext
    public class Execute {

        @Mock
        LogPreloader preloader;

        @InjectMocks
        LogsCommandExecutor executor;

        @Test
        public void testLogFormat() {
            Mockito.when(preloader.getPreloadedLogs())
                    .thenReturn(Arrays.asList(new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogsCommandExecutorTest.class), Level.ERROR, "Message", new Throwable(), new String[]{}), new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogsCommandExecutorTest.class), Level.WARN, "Message {}", new Throwable(), new String[]{"Foo"}), new LoggingEvent("com.dummy.Method", (Logger) LoggerFactory.getLogger(LogsCommandExecutorTest.class), Level.ERROR, "Exception", new RuntimeException(), new String[]{})));

            LogsCommand command = new LogsCommand();
            command.setCommandId(UUID.randomUUID());
            LogsCommand.Response response = (LogsCommand.Response) executor.execute(command);

            assertThat(response.getResult().split("\\n")).hasSize(3);

            // should contain log levels
            assertThat(response.getResult()).contains("WARN");
            assertThat(response.getResult()).contains("ERROR");

            // should contain messages
            assertThat(response.getResult()).contains("Message");
            assertThat(response.getResult()).contains("Message Foo");
            assertThat(response.getResult()).contains("Exception");

            // should contain the exception
            assertThat(response.getResult()).contains("RuntimeException");

            // should contain class that logged the message
            assertThat(response.getResult()).contains("LogsCommandExecutorTest");

            // TODO: check if timestamps are present in the appropriate format
        }

    }

}
