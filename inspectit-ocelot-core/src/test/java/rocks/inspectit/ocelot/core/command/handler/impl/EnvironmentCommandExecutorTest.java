package rocks.inspectit.ocelot.core.command.handler.impl;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.commons.models.command.impl.EnvironmentCommand;
import rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
public class EnvironmentCommandExecutorTest {

    static final String tmpDir = "tmpconf_int_test";

    static final String configurationA = "inspectit:\n  agent-commands:\n    enabled: true\n    url: http://localhost:8090/api/v1/agent/command";

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
            testConfiguration(configurationA, true, new URL("http://localhost:8090/api/v1/agent/command"));
        }

        private void testConfiguration(String configuration, boolean enabled, URL commandUrl) throws IOException {
            FileUtils.write(new File(tmpDir + "/env-command.yml"), configuration, Charset.defaultCharset());

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
                AgentCommandSettings settings = env.getCurrentConfig().getAgentCommands();
                assertThat(settings).isNotNull();
                assertThat(settings.isEnabled()).isEqualTo(enabled);
                assertThat(settings.getUrl()).isEqualTo(commandUrl);
            });
        }

    }

    @Nested
    @DirtiesContext
    public class Execute {

        @InjectMocks
        EnvironmentCommandExecutor executor;

        @Test
        public void testEnvironmentDetailsBeingBuiltCorrectly() {

            EnvironmentCommand command = new EnvironmentCommand();
            command.setCommandId(UUID.randomUUID());
            EnvironmentCommand.Response response = (EnvironmentCommand.Response) executor.execute(command);

            // available environment variables should be returned
            assertThat(response.getEnvironment().getEnvironmentVariables().size()).isGreaterThan(0);
            assertThat(response.getEnvironment().getEnvironmentVariables()).containsKey("PATH");

            // system properties should be returned
            assertThat(response.getEnvironment().getSystemProperties().size()).isGreaterThan(0);
            assertThat(response.getEnvironment().getSystemProperties()).containsKey("os.name");
            assertThat(response.getEnvironment().getSystemProperties()).containsKey("java.version");

            // any jvm arguments passed in the starting command should be returned
            assertThat(response.getEnvironment().getJvmArguments().size()).isGreaterThan(0);
        }

        @Test
        public void testInvalidCommand() {
            LogsCommand command = new LogsCommand();
            command.setCommandId(UUID.randomUUID());

            IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> executor.execute(command));
            assertThat(exception.getMessage()).isEqualTo("Invalid command type. Executor does not support commands of type " + command.getClass());
        }

    }

}
