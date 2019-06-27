package rocks.inspectit.ocelot.utils;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ObjectMapperUtilsTest {

    private static File tempDirectory;

    @InjectMocks
    ObjectMapperUtils mapper;

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

            mapper.postConstruct();
            mapper.writeAgentMappings(Collections.singletonList(mapping), testFile);

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

            mapper.postConstruct();
            List<AgentMapping> result = mapper.readAgentMappings(testFile);

            assertThat(result).hasSize(1);
            AgentMapping mapping = result.get(0);
            assertThat(mapping.getName()).isEqualTo("mapping");
            assertThat(mapping.getSources()).containsExactly("/any-source");
            assertThat(mapping.getAttributes()).containsEntry("key", "val");
        }
    }
}