package rocks.inspectit.ocelot.agentconfiguration;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.mappings.AgentMappingSerializer;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static rocks.inspectit.ocelot.file.versioning.Branch.WORKSPACE;

@ExtendWith(MockitoExtension.class)
public class AgentConfigurationReloadTaskTest {

    @Mock
    AgentMappingSerializer serializer;

    @Mock
    FileManager fileManager;

    @Mock
    RevisionAccess liveAccessor;

    @Mock
    RevisionAccess workspaceAccessor;

    @BeforeEach
    void beforeEach() {
        lenient().when(fileManager.getWorkspaceRevision()).thenReturn(workspaceAccessor);
        lenient().when(fileManager.getLiveRevision()).thenReturn(liveAccessor);
        lenient().when(serializer.getRevisionAccess()).thenReturn(workspaceAccessor);
    }

    @Nested
    class Run {

        @Test
        void loadWithException() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"), Stream.of("/test.yml"));
            when(workspaceAccessor.agentMappingsExist()).thenReturn(true);
            when(workspaceAccessor.configurationFileExists(anyString())).thenReturn(true);
            when(workspaceAccessor.configurationFileIsDirectory(anyString())).thenReturn(true);
            when(workspaceAccessor.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            // the first call will return a broken file
            when(workspaceAccessor.readConfigurationFile(anyString())).thenReturn(Optional.of("key:\tbroken"), Optional.of("key: valid"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();
            AgentMapping mapping2 = AgentMapping.builder()
                    .name("test2")
                    .source("/test2")
                    .sourceBranch(WORKSPACE)
                    .build();
            doReturn(Arrays.asList(mapping, mapping2)).when(serializer).readAgentMappings(any());

            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);

            task.run();

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).hasSize(1);
            assertThat(configurationList).element(0)
                    .extracting(AgentConfiguration::getConfigYaml)
                    .isEqualTo("{key: valid}\n");
        }

        @Test
        void loadWithExceptionOnlyString() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"), Stream.of("/test.yml"));
            when(workspaceAccessor.agentMappingsExist()).thenReturn(true);
            when(workspaceAccessor.configurationFileExists(anyString())).thenReturn(true);
            when(workspaceAccessor.configurationFileIsDirectory(anyString())).thenReturn(true);
            when(workspaceAccessor.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            // the first call will return an invalid file only containing a string
            when(workspaceAccessor.readConfigurationFile(anyString())).thenReturn(Optional.of("onlystring"), Optional.of("key: valid"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();
            AgentMapping mapping2 = AgentMapping.builder()
                    .name("test2")
                    .source("/test2")
                    .sourceBranch(WORKSPACE)
                    .build();
            doReturn(Arrays.asList(mapping, mapping2)).when(serializer).readAgentMappings(any());

            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);

            task.run();

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).hasSize(1);
            assertThat(configurationList).element(0)
                    .extracting(AgentConfiguration::getConfigYaml)
                    .isEqualTo("{key: valid}\n");
        }

        @Test
        void loadWithExceptionOnlyList() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"), Stream.of("/test.yml"));
            when(workspaceAccessor.agentMappingsExist()).thenReturn(true);
            when(workspaceAccessor.configurationFileExists(anyString())).thenReturn(true);
            when(workspaceAccessor.configurationFileIsDirectory(anyString())).thenReturn(true);
            when(workspaceAccessor.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            // the first call will return an invalid file only containing a list
            when(workspaceAccessor.readConfigurationFile(anyString())).thenReturn(Optional.of("- listentry1\n  listentry2"), Optional.of("key: valid"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();
            AgentMapping mapping2 = AgentMapping.builder()
                    .name("test2")
                    .source("/test2")
                    .sourceBranch(WORKSPACE)
                    .build();
            doReturn(Arrays.asList(mapping, mapping2)).when(serializer).readAgentMappings(any());

            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);

            task.run();

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).hasSize(1);
            assertThat(configurationList).element(0)
                    .extracting(AgentConfiguration::getConfigYaml)
                    .isEqualTo("{key: valid}\n");
        }

        @Test
        void loadMappingFromWorkspace() {
            when(workspaceAccessor.agentMappingsExist()).thenReturn(true);

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();
            doReturn(Collections.singletonList(mapping)).when(serializer).readAgentMappings(any());
            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);
            task.run();

            verify(serializer, times(1)).readAgentMappings(workspaceAccessor);
            verify(serializer, times(0)).readAgentMappings(liveAccessor);
        }

        @Test
        void loadMappingFromLive() {
            lenient().when(serializer.getRevisionAccess()).thenReturn(liveAccessor);
            when(liveAccessor.agentMappingsExist()).thenReturn(true);

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();
            doReturn(Collections.singletonList(mapping)).when(serializer).readAgentMappings(any());
            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);
            task.run();

            verify(serializer, times(0)).readAgentMappings(workspaceAccessor);
            verify(serializer, times(1)).readAgentMappings(liveAccessor);
        }
    }
}
