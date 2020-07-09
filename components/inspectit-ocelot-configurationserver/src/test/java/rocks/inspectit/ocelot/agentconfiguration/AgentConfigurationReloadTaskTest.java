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
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
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
    FileManager fileManager;

    @Mock
    AbstractFileAccessor fileAccessor;

    @BeforeEach
    public void beforeEach() {
        lenient().when(fileManager.getLiveRevision()).thenReturn(fileAccessor);
    }

    @Nested
    class Run {

        @Test
        public void loadWithException() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"), Stream.of("/test.yml"));
            when(fileAccessor.configurationFileExists(anyString())).thenReturn(true);
            when(fileAccessor.configurationFileIsDirectory(anyString())).thenReturn(true);
            when(fileAccessor.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            // the first call will return a broken file
            when(fileAccessor.readConfigurationFile(anyString())).thenReturn(Optional.of("key:\tbroken"), Optional.of("key: valid"));

            AgentMapping mapping = AgentMapping.builder().name("test").source("/test").build();
            AgentMapping mapping2 = AgentMapping.builder().name("test2").source("/test2").build();

            final MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            Consumer<List<AgentConfiguration>> consumer = configurations::setValue;

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(Arrays.asList(mapping, mapping2), fileManager, consumer);

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
            when(fileAccessor.configurationFileExists("test")).thenReturn(true);
            when(fileAccessor.configurationFileIsDirectory("test")).thenReturn(true);
            when(fileAccessor.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(fileAccessor.readConfigurationFile("/test.yml")).thenReturn(Optional.of("key: value"));

            AgentMapping mapping = AgentMapping.builder().name("test").source("/test").build();
            String string = reloadTask.loadConfigForMapping(mapping);

            assertThat(string).isEqualTo("{key: value}\n");
        }

        @Test
        public void yamlWithTab() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"));
            when(fileAccessor.configurationFileExists("test")).thenReturn(true);
            when(fileAccessor.configurationFileIsDirectory("test")).thenReturn(true);
            when(fileAccessor.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(fileAccessor.readConfigurationFile("/test.yml")).thenReturn(Optional.of("key:\tvalue"));

            AgentMapping mapping = AgentMapping.builder().name("test").source("/test").build();

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
        void nonExistingSourcesSpecified() throws IOException {
            doReturn(false).when(fileAccessor).configurationFileExists("a.yml");
            doReturn(false).when(fileAccessor).configurationFileExists("some/folder");

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("a.yml")
                            .source("/some/folder")
                            .build());

            assertThat(result).isEmpty();
        }


        @Test
        void nonYamlIgnored() throws IOException {
            doReturn(true).when(fileAccessor).configurationFileExists(any());
            doReturn(false).when(fileAccessor).configurationFileIsDirectory(any());
            doReturn(Optional.of("")).when(fileAccessor).readConfigurationFile(any());

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("a.yml")
                            .source("b.YmL")
                            .source("c.yaml")
                            .source("d.txt")
                            .build());

            assertThat(result).isEmpty();
            verify(fileAccessor).readConfigurationFile("a.yml");
            verify(fileAccessor).readConfigurationFile("b.YmL");
            verify(fileAccessor).readConfigurationFile("c.yaml");

            verify(fileAccessor, never()).readConfigurationFile("d.txt");
        }


        @Test
        void leadingSlashesInSourcesRemoved() throws IOException {
            doReturn(false).when(fileAccessor).configurationFileExists("a.yml");

            lenient().doThrow(new RuntimeException()).when(fileAccessor).configurationFileExists(startsWith("/"));

            reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/a.yml")
                            .build());

            verify(fileAccessor).configurationFileExists(eq("a.yml"));
        }


        @Test
        void priorityRespected() throws IOException {

            when(fileAccessor.configurationFileExists(any())).thenReturn(true);

            doReturn(true).when(fileAccessor).configurationFileIsDirectory("folder");
            doReturn(false).when(fileAccessor).configurationFileIsDirectory("z.yml");

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

            when(fileAccessor.listConfigurationFiles("folder")).thenReturn(fileInfos);

            doReturn(Optional.of("{ val1: z}")).when(fileAccessor).readConfigurationFile("z.yml");
            doReturn(Optional.of("{ val1: a, val2: a}")).when(fileAccessor).readConfigurationFile("folder/a.yml");
            doReturn(Optional.of("{ val1: b, val2: b, val3: b}")).when(fileAccessor).readConfigurationFile("folder/b.yml");

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/z.yml")
                            .source("/folder")
                            .build());


            assertThat(result).isEqualTo("{val1: z, val2: a, val3: b}\n");
            verify(fileAccessor, never()).readConfigurationFile("folder/somethingelse");
        }
    }
}
