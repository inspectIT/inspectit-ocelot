package rocks.inspectit.ocelot.agentconfiguration;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.Branch;
import rocks.inspectit.ocelot.mappings.AgentMappingSerializer;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentConfigurationReloadTaskTest {

    @InjectMocks
    AgentConfigurationReloadTask reloadTask;

    @Mock
    AgentMappingSerializer serializer;

    @Mock
    FileManager fileManager;

    @Mock
    RevisionAccess liveAccessor;

    @Mock
    RevisionAccess workspaceAccessor;

    @BeforeEach
    public void beforeEach() {
        lenient().when(fileManager.getWorkspaceRevision()).thenReturn(workspaceAccessor);
        lenient().when(fileManager.getLiveRevision()).thenReturn(liveAccessor);
    }

    @Nested
    class Run {

        @Test
        public void loadWithException() {
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
                    .sourceBranch(Branch.WORKSPACE)
                    .build();
            AgentMapping mapping2 = AgentMapping.builder()
                    .name("test2")
                    .source("/test2")
                    .sourceBranch(Branch.WORKSPACE)
                    .build();
            doReturn(Arrays.asList(mapping, mapping2)).when(serializer).readAgentMappings(any());

            MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(serializer, fileManager, consumer);

            task.run();

            List<AgentConfiguration> configurationList = configurations.getValue();
            assertThat(configurationList).hasSize(1);
            assertThat(configurationList)
                    .element(0)
                    .extracting(AgentConfiguration::getConfigYaml)
                    .isEqualTo("{key: valid}\n");
        }
    }

    @Nested
    class LoadAndMergeYaml {

        @Test
        public void loadYaml() throws IOException {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"));
            when(workspaceAccessor.configurationFileExists("test")).thenReturn(true);
            when(workspaceAccessor.configurationFileIsDirectory("test")).thenReturn(true);
            when(workspaceAccessor.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(workspaceAccessor.readConfigurationFile("/test.yml")).thenReturn(Optional.of("key: value"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(Branch.WORKSPACE)
                    .build();
            String string = reloadTask.loadConfigForMapping(mapping);

            assertThat(string).isEqualTo("{key: value}\n");
        }

        @Test
        public void yamlWithTab() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"));
            when(workspaceAccessor.configurationFileExists("test")).thenReturn(true);
            when(workspaceAccessor.configurationFileIsDirectory("test")).thenReturn(true);
            when(workspaceAccessor.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(workspaceAccessor.readConfigurationFile("/test.yml")).thenReturn(Optional.of("key:\tvalue"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(Branch.WORKSPACE)
                    .build();

            assertThatExceptionOfType(AgentConfigurationReloadTask.InvalidConfigurationFileException.class)
                    .isThrownBy(() -> reloadTask.loadConfigForMapping(mapping))
                    .withMessage("The configuration file '/test.yml' is invalid and cannot be parsed.");
        }
    }

    @Nested
    class LoadConfigForMapping {

        @Test
        void noSourcesSpecified() throws IOException {
            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .build());

            assertThat(result).isEmpty();
        }

        @Test
        void liveBranchSpecified() throws IOException {
            AgentMapping mapping = AgentMapping.builder()
                    .source("a.yml")
                    .sourceBranch(Branch.LIVE)
                    .build();

            doReturn(true).when(liveAccessor).configurationFileExists("a.yml");
            doReturn(false).when(liveAccessor).configurationFileIsDirectory("a.yml");
            doReturn(Optional.of("key: value")).when(liveAccessor).readConfigurationFile("a.yml");

            String result = reloadTask.loadConfigForMapping(mapping);

            assertThat(result).isEqualToIgnoringWhitespace("{key: value}");
        }

        @Test
        void nonExistingSourcesSpecified() throws IOException {
            doReturn(false).when(workspaceAccessor).configurationFileExists("a.yml");
            doReturn(false).when(workspaceAccessor).configurationFileExists("some/folder");

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("a.yml")
                            .source("/some/folder")
                            .sourceBranch(Branch.WORKSPACE)
                            .build());

            assertThat(result).isEmpty();
        }

        @Test
        void nonYamlIgnored() throws IOException {
            doReturn(true).when(workspaceAccessor).configurationFileExists(any());
            doReturn(false).when(workspaceAccessor).configurationFileIsDirectory(any());
            doReturn(Optional.of("")).when(workspaceAccessor).readConfigurationFile(any());

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("a.yml")
                            .source("b.YmL")
                            .source("c.yaml")
                            .source("d.txt")
                            .sourceBranch(Branch.WORKSPACE)
                            .build());

            assertThat(result).isEmpty();
            verify(workspaceAccessor).readConfigurationFile("a.yml");
            verify(workspaceAccessor).readConfigurationFile("b.YmL");
            verify(workspaceAccessor).readConfigurationFile("c.yaml");

            verify(workspaceAccessor, never()).readConfigurationFile("d.txt");
        }

        @Test
        void leadingSlashesInSourcesRemoved() throws IOException {
            doReturn(false).when(workspaceAccessor).configurationFileExists("a.yml");

            lenient().doThrow(new RuntimeException()).when(workspaceAccessor).configurationFileExists(startsWith("/"));

            reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/a.yml")
                            .sourceBranch(Branch.WORKSPACE)
                            .build());

            verify(workspaceAccessor).configurationFileExists(eq("a.yml"));
        }

        @Test
        void priorityRespected() throws IOException {

            when(workspaceAccessor.configurationFileExists(any())).thenReturn(true);

            doReturn(true).when(workspaceAccessor).configurationFileIsDirectory("folder");
            doReturn(false).when(workspaceAccessor).configurationFileIsDirectory("z.yml");

            List<FileInfo> fileInfos = Arrays.asList(
                    FileInfo.builder()
                            .type(FileInfo.Type.FILE)
                            .name("b.yml")
                            .build(),
                    FileInfo.builder()
                            .type(FileInfo.Type.FILE)
                            .name("a.yml")
                            .build(),
                    FileInfo.builder()
                            .type(FileInfo.Type.FILE)
                            .name("somethingelse")
                            .build()
            );

            when(workspaceAccessor.listConfigurationFiles("folder")).thenReturn(fileInfos);

            doReturn(Optional.of("{ val1: z}")).when(workspaceAccessor).readConfigurationFile("z.yml");
            doReturn(Optional.of("{ val1: a, val2: a}")).when(workspaceAccessor).readConfigurationFile("folder/a.yml");
            doReturn(Optional.of("{ val1: b, val2: b, val3: b}")).when(workspaceAccessor)
                    .readConfigurationFile("folder/b.yml");

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/z.yml")
                            .source("/folder")
                            .sourceBranch(Branch.WORKSPACE)
                            .build());

            assertThat(result).isEqualTo("{val1: z, val2: a, val3: b}\n");
            verify(workspaceAccessor, never()).readConfigurationFile("folder/somethingelse");
        }
    }
}
