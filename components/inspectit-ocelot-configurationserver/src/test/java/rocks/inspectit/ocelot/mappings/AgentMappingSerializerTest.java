package rocks.inspectit.ocelot.mappings;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AgentMappingSerializerTest {

    private static File tempDirectory;

    @InjectMocks
    AgentMappingSerializer serializer;

    @BeforeAll
    public static void beforeAll() throws IOException {
        tempDirectory = Files.createTempDirectory("mapper-test").toFile();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        FileUtils.deleteDirectory(tempDirectory);
    }

    @AfterEach
    public void afterEach() throws IOException {
        FileUtils.cleanDirectory(tempDirectory);
    }

    @Nested
    public class WriteAgentMappings {

        @Test
        public void successfullyWriteYaml() throws IOException {
            AgentMapping mapping = AgentMapping.builder().name("mapping").source("/any-source").attribute("key", "val").build();
            File testFile = new File(tempDirectory, "test.yml");

            serializer.postConstruct();
            serializer.writeAgentMappings(Collections.singletonList(mapping), testFile);

            String content = new String(Files.readAllBytes(testFile.toPath()));
            assertThat(content).isEqualTo("- name: \"mapping\"\n" +
                    "  sources:\n" +
                    "  - \"/any-source\"\n" +
                    "  attributes:\n" +
                    "    key: \"val\"\n");
        }
    }

    @Nested
    public class ReadAgentMappings {

        @Test
        public void successfullyReadYaml() throws IOException {
            String dummyYaml = "- name: \"mapping\"\n" +
                    "  sources:\n" +
                    "  - \"/any-source\"\n" +
                    "  attributes:\n" +
                    "    key: \"val\"\n";
            File testFile = new File(tempDirectory, "test.yml");
            Files.write(testFile.toPath(), dummyYaml.getBytes());

            serializer.postConstruct();
            List<AgentMapping> result = serializer.readAgentMappings(testFile);

            assertThat(result).hasSize(1);
            AgentMapping mapping = result.get(0);
            assertThat(mapping.getName()).isEqualTo("mapping");
            assertThat(mapping.getSources()).containsExactly("/any-source");
            assertThat(mapping.getAttributes()).containsEntry("key", "val");
        }
    }
}