package rocks.inspectit.ocelot.agentconfiguration;

import inspectit.ocelot.configdocsgenerator.model.AgentDocumentation;
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

    final String file = "/test.yml";

    @BeforeEach
    void beforeEach() {
        lenient().when(fileManager.getWorkspaceRevision()).thenReturn(workspaceAccessor);
        lenient().when(fileManager.getLiveRevision()).thenReturn(liveAccessor);
        lenient().when(serializer.getRevisionAccess()).thenReturn(workspaceAccessor);
        lenient().when(workspaceAccessor.agentMappingsExist()).thenReturn(true);

        FileInfo fileInfo = mock(FileInfo.class);
        lenient().when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of(file), Stream.of(file));
        lenient().when(workspaceAccessor.configurationFileExists(anyString())).thenReturn(true);
        lenient().when(workspaceAccessor.configurationFileIsDirectory(anyString())).thenReturn(true);
        lenient().when(workspaceAccessor.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
    }

    @Nested
    class Run {

        @Test
        void runTaskWithValidOutput() throws Exception {
            when(workspaceAccessor.readConfigurationFile(anyString())).thenReturn(Optional.of("key: valid"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();
            doReturn(Collections.singletonList(mapping)).when(serializer).readAgentMappings(any());

            AgentDocumentation documentation = new AgentDocumentation(file, Collections.emptySet());

            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;
            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);

            task.run();

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).isNotEmpty();

            AgentConfiguration configuration = configurationList.get(0);

            assertThat(configuration.getMapping()).isEqualTo(mapping);
            assertThat(configuration.getConfigYaml()).isEqualTo("{key: valid}\n");
            assertThat(configuration.getDocumentationSuppliers()).isNotEmpty();

            AgentDocumentationSupplier supplier = configuration.getDocumentationSuppliers().iterator().next();

            assertThat(supplier.get()).isEqualTo(documentation);
        }

        @Test
        void runTaskWithNullMapping() {
            doReturn(Collections.singletonList(null)).when(serializer).readAgentMappings(any());

            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);

            task.run();

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).isEmpty();
        }

        @Test
        void runTaskWithoutFileAccessor() {
            when(fileManager.getWorkspaceRevision()).thenReturn(null);
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

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).isEmpty();
        }

        @Test
        void loadTabWithException() {
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

            AgentDocumentation documentation = new AgentDocumentation(file, Collections.emptySet());

            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);

            task.run();

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).hasSize(1);

            AgentConfiguration configuration = configurationList.get(0);

            assertThat(configuration.getMapping()).isEqualTo(mapping2);
            assertThat(configuration.getConfigYaml()).isEqualTo("{key: valid}\n");
            assertThat(configuration.getDocumentationSuppliers()).hasSize(1);

            AgentDocumentationSupplier supplier = configuration.getDocumentationSuppliers().iterator().next();

            assertThat(supplier.get()).isEqualTo(documentation);
        }

        @Test
        void loadOnlyStringWithException() {
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

            AgentDocumentation documentation = new AgentDocumentation(file, Collections.emptySet());

            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);

            task.run();

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).hasSize(1);

            AgentConfiguration configuration = configurationList.get(0);

            assertThat(configuration.getMapping()).isEqualTo(mapping2);
            assertThat(configuration.getConfigYaml()).isEqualTo("{key: valid}\n");
            assertThat(configuration.getDocumentationSuppliers()).hasSize(1);

            AgentDocumentationSupplier supplier = configuration.getDocumentationSuppliers().iterator().next();

            assertThat(supplier.get()).isEqualTo(documentation);
        }

        @Test
        void loadOnlyListWithException() {
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

            AgentDocumentation documentation = new AgentDocumentation(file, Collections.emptySet());

            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);

            task.run();

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).hasSize(1);

            AgentConfiguration configuration = configurationList.get(0);

            assertThat(configuration.getMapping()).isEqualTo(mapping2);
            assertThat(configuration.getConfigYaml()).isEqualTo("{key: valid}\n");
            assertThat(configuration.getDocumentationSuppliers()).hasSize(1);

            AgentDocumentationSupplier supplier = configuration.getDocumentationSuppliers().iterator().next();

            assertThat(supplier.get()).isEqualTo(documentation);
        }
    }

    @Nested
    class MappingRevisionAccess {

        @Test
        void loadMappingFromWorkspace() {
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
            when(serializer.getRevisionAccess()).thenReturn(liveAccessor);
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
