package rocks.inspectit.ocelot.agentconfig;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentConfigurationManagerTest {

    @Mock
    FileManager fileManager;

    @Mock
    AgentMappingManager mappingManager;

    @InjectMocks
    AgentConfigurationManager configManager;


    @Nested
    class GetConfiguration {

        @Test
        void noMatchingMapping() throws IOException {
            doReturn(Arrays.asList(
                    AgentMapping.builder()
                            .attribute("service", "test-\\d+")
                            .build()))
                    .when(mappingManager).getAgentMappings();


            String result = configManager.getConfiguration(ImmutableMap.of("service", "somethingElse"));

            assertThat(result).isNull();
        }

        @Test
        void priorityRespected() throws IOException {
            doReturn(Arrays.asList(
                    AgentMapping.builder()
                            .attribute("service", "test")
                            .source("test.yml")
                            .build(),
                    AgentMapping.builder()
                            .attribute("service", ".*")
                            .source("default.yml")
                            .build()))
                    .when(mappingManager).getAgentMappings();

            doReturn(true).when(fileManager).doesPathExist(any());
            doReturn(false).when(fileManager).isDirectory(any());
            doReturn("a: test").when(fileManager).readFile("test.yml");
            doReturn("a: default").when(fileManager).readFile("default.yml");

            String resultA = configManager.getConfiguration(ImmutableMap.of("service", "test"));
            String resultB = configManager.getConfiguration(ImmutableMap.of("service", "somethingElse"));

            assertThat(resultA).isEqualTo("{a: test}\n");
            assertThat(resultB).isEqualTo("{a: default}\n");
        }


        @Test
        void multipleAttributesChecked() throws IOException {
            doReturn(Arrays.asList(
                    AgentMapping.builder()
                            .attribute("service", "test-\\d+")
                            .attribute("application", "myApp")
                            .source("test.yml")
                            .build()))
                    .when(mappingManager).getAgentMappings();

            doReturn(true).when(fileManager).doesPathExist(any());
            doReturn(false).when(fileManager).isDirectory(any());
            doReturn("a: test").when(fileManager).readFile("test.yml");

            String resultA = configManager.getConfiguration(ImmutableMap.of("service", "test-17", "application", "myApp"));
            String resultB = configManager.getConfiguration(ImmutableMap.of("service", "test-17"));
            String resultC = configManager.getConfiguration(ImmutableMap.of("service", "test-17", "application", "foo"));

            assertThat(resultA).isNotNull();
            assertThat(resultB).isNull();
            assertThat(resultC).isNull();
        }

    }

    @Nested
    class LoadConfigForMapping {

        @Test
        void noSourcesSpecified() throws IOException {
            String result = configManager.loadConfigForMapping(
                    AgentMapping.builder()
                            .build());

            assertThat(result).isEmpty();
        }


        @Test
        void nonExistingSourcesSpecified() throws IOException {
            doReturn(false).when(fileManager).doesPathExist("a.yml");
            doReturn(false).when(fileManager).doesPathExist("some/folder");

            String result = configManager.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("a.yml")
                            .source("/some/folder")
                            .build());

            assertThat(result).isEmpty();
        }


        @Test
        void nonYamlIgnored() throws IOException {
            doReturn(true).when(fileManager).doesPathExist(any());
            doReturn(false).when(fileManager).isDirectory(any());
            doReturn("").when(fileManager).readFile(any());

            String result = configManager.loadConfigForMapping(
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
            doReturn(false).when(fileManager).doesPathExist("a.yml");

            lenient().doThrow(new RuntimeException()).when(fileManager).doesPathExist(startsWith("/"));

            configManager.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/a.yml")
                            .build());

            verify(fileManager).doesPathExist(eq("a.yml"));
        }


        @Test
        void priorityRespected() throws IOException {

            doReturn(true).when(fileManager).doesPathExist(any());

            doReturn(true).when(fileManager).isDirectory("folder");
            doReturn(false).when(fileManager).isDirectory("z.yml");

            doReturn(Arrays.asList(
                    FileInfo.builder()
                            .type(FileInfo.Type.FILE)
                            .path("folder/b.yml")
                            .build(),
                    FileInfo.builder()
                            .type(FileInfo.Type.FILE)
                            .path("folder/a.yml")
                            .build(),
                    FileInfo.builder()
                            .type(FileInfo.Type.FILE)
                            .path("folder/somethingelse")
                            .build()

            )).when(fileManager).getFilesInDirectory("folder");

            doReturn("{ val1: z}").when(fileManager).readFile("z.yml");
            doReturn("{ val1: a, val2: a}").when(fileManager).readFile("folder/a.yml");
            doReturn("{ val1: b, val2: b, val3: b}").when(fileManager).readFile("folder/b.yml");


            String result = configManager.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/z.yml")
                            .source("/folder")
                            .build());


            assertThat(result).isEqualTo("{val1: z, val2: a, val3: b}\n");
            verify(fileManager, never()).readFile("folder/somethingelse");
        }
    }
}
