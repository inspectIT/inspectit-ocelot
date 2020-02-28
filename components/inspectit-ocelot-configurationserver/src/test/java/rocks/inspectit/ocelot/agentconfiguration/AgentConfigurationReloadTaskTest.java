package rocks.inspectit.ocelot.agentconfiguration;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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

    @Nested
    class Run {

        @Test
        public void loadWithException() throws Exception {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"), Stream.of("/test.yml"));
            when(fileManager.exists(anyString())).thenReturn(true);
            when(fileManager.isDirectory(anyString())).thenReturn(true);
            when(fileManager.getFilesInDirectory(anyString(), anyBoolean())).thenReturn(Arrays.asList(fileInfo));
            // the first call will return a broken file
            when(fileManager.readFile(anyString())).thenReturn("key:\tbroken", "key: valid");

            AgentMapping mapping = AgentMapping.builder().name("test").source("/test").build();
            AgentMapping mapping2 = AgentMapping.builder().name("test2").source("/test2").build();

            final MutableObject<List<AgentConfiguration>> configurations = new MutableObject<>();
            final CountDownLatch latch = new CountDownLatch(1);
            Consumer<List<AgentConfiguration>> consumer = (newConfigurations) -> {
                configurations.setValue(newConfigurations);
                latch.countDown();
            };

            AgentConfigurationReloadTask task = new AgentConfigurationReloadTask(Arrays.asList(mapping, mapping2), fileManager, consumer);

            task.run();

            latch.await();

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
            when(fileManager.exists("test")).thenReturn(true);
            when(fileManager.isDirectory("test")).thenReturn(true);
            when(fileManager.getFilesInDirectory(anyString(), anyBoolean())).thenReturn(Arrays.asList(fileInfo));
            when(fileManager.readFile("/test.yml")).thenReturn("key: value");

            AgentMapping mapping = AgentMapping.builder().name("test").source("/test").build();
            String string = reloadTask.loadConfigForMapping(mapping);

            assertThat(string).isEqualTo("{key: value}\n");
        }

        @Test
        public void yamlWithTab() throws IOException {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"));
            when(fileManager.exists("test")).thenReturn(true);
            when(fileManager.isDirectory("test")).thenReturn(true);
            when(fileManager.getFilesInDirectory(anyString(), anyBoolean())).thenReturn(Arrays.asList(fileInfo));
            when(fileManager.readFile("/test.yml")).thenReturn("key:\tvalue");

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
            doReturn(false).when(fileManager).exists("a.yml");
            doReturn(false).when(fileManager).exists("some/folder");

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("a.yml")
                            .source("/some/folder")
                            .build());

            assertThat(result).isEmpty();
        }


        @Test
        void nonYamlIgnored() throws IOException {
            doReturn(true).when(fileManager).exists(any());
            doReturn(false).when(fileManager).isDirectory(any());
            doReturn("").when(fileManager).readFile(any());

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("a.yml")
                            .source("b.YmL")
                            .source("c.yaml")
                            .source("d.txt")
                            .build());

            assertThat(result).isEmpty();
            verify(fileManager).readFile("a.yml");
            verify(fileManager).readFile("b.YmL");
            verify(fileManager).readFile("c.yaml");

            verify(fileManager, never()).readFile("d.txt");
        }


        @Test
        void leadingSlashesInSourcesRemoved() throws IOException {
            doReturn(false).when(fileManager).exists("a.yml");

            lenient().doThrow(new RuntimeException()).when(fileManager).exists(startsWith("/"));

            reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/a.yml")
                            .build());

            verify(fileManager).exists(eq("a.yml"));
        }


        @Test
        void priorityRespected() throws IOException {

            doReturn(true).when(fileManager).exists(any());

            doReturn(true).when(fileManager).isDirectory("folder");
            doReturn(false).when(fileManager).isDirectory("z.yml");

            doReturn(Arrays.asList(
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

            )).when(fileManager).getFilesInDirectory("folder", true);

            doReturn("{ val1: z}").when(fileManager).readFile("z.yml");
            doReturn("{ val1: a, val2: a}").when(fileManager).readFile("folder/a.yml");
            doReturn("{ val1: b, val2: b, val3: b}").when(fileManager).readFile("folder/b.yml");


            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/z.yml")
                            .source("/folder")
                            .build());


            assertThat(result).isEqualTo("{val1: z, val2: a, val3: b}\n");
            verify(fileManager, never()).readFile("folder/somethingelse");
        }
    }
}
