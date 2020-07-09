package rocks.inspectit.ocelot.agentconfiguration;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.mappings.AgentMappingSerializer;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentConfigurationManagerTest {

    @InjectMocks
    AgentConfigurationManager configManager;

    @Mock
    FileManager fileManager;

    @Mock
    AgentMappingSerializer serializer;

    @Mock
    ExecutorService executor;

    @Mock
    RevisionAccess fileAccessor;

    @BeforeEach
    public void beforeEach() {
        lenient().when(fileManager.getWorkspaceRevision()).thenReturn(fileAccessor);
    }

    void init() {
        doAnswer(a -> {
            ((Runnable) a.getArgument(0)).run();
            return null;
        }).when(executor).submit(any(Runnable.class));

        configManager.config = InspectitServerSettings.builder()
                .agentEvictionDelay(Duration.ofDays(1))
                .maxAgents(1000)
                .build();
        configManager.init();
    }

    @Nested
    class GetConfiguration {

        @Test
        void noMatchingMapping() {

            doReturn(true).when(fileAccessor).agentMappingsExist();
            doReturn(Arrays.asList(
                    AgentMapping.builder()
                            .attribute("service", "test-\\d+")
                            .build()))
                    .when(serializer).readAgentMappings(any());

            init();

            AgentConfiguration result = configManager.getConfiguration(ImmutableMap.of("service", "somethingElse"));

            assertThat(result).isNull();
        }

        @Test
        void priorityRespected() {
            doReturn(Arrays.asList(
                    AgentMapping.builder()
                            .attribute("service", "test")
                            .source("test.yml")
                            .build(),
                    AgentMapping.builder()
                            .attribute("service", ".*")
                            .source("default.yml")
                            .build()))
                    .when(serializer).readAgentMappings(any());

            doReturn(true).when(fileAccessor).agentMappingsExist();
            doReturn(true).when(fileAccessor).configurationFileExists(any());
            doReturn(false).when(fileAccessor).configurationFileIsDirectory(any());
            doReturn(Optional.of("a: test")).when(fileAccessor).readConfigurationFile("test.yml");
            doReturn(Optional.of("a: default")).when(fileAccessor).readConfigurationFile("default.yml");

            init();

            AgentConfiguration resultA = configManager.getConfiguration(ImmutableMap.of("service", "test"));
            AgentConfiguration resultB = configManager.getConfiguration(ImmutableMap.of("service", "somethingElse"));

            assertThat(resultA.getConfigYaml()).isEqualTo("{a: test}\n");
            assertThat(resultB.getConfigYaml()).isEqualTo("{a: default}\n");
        }

        @Test
        void multipleAttributesChecked() {
            doReturn(Arrays.asList(
                    AgentMapping.builder()
                            .attribute("service", "test-\\d+")
                            .attribute("application", "myApp")
                            .source("test.yml")
                            .build()))
                    .when(serializer).readAgentMappings(any());

            doReturn(true).when(fileAccessor).agentMappingsExist();
            doReturn(true).when(fileAccessor).configurationFileExists(any());
            doReturn(false).when(fileAccessor).configurationFileIsDirectory(any());
            doReturn(Optional.of("a: test")).when(fileAccessor).readConfigurationFile("test.yml");

            init();

            AgentConfiguration resultA = configManager.getConfiguration(ImmutableMap.of("service", "test-17", "application", "myApp"));
            AgentConfiguration resultB = configManager.getConfiguration(ImmutableMap.of("service", "test-17"));
            AgentConfiguration resultC = configManager.getConfiguration(ImmutableMap.of("service", "test-17", "application", "foo"));

            assertThat(resultA).isNotNull();
            assertThat(resultB).isNull();
            assertThat(resultC).isNull();
        }

    }
}
