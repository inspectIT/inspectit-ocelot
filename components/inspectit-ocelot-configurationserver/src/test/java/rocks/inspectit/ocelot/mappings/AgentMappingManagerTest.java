package rocks.inspectit.ocelot.mappings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AgentMappingManagerTest {

    @InjectMocks
    AgentMappingManager manager;

    Path tempDirectory;

    @BeforeEach
    public void beforeEach() throws IOException {
        tempDirectory = Files.createTempDirectory("agent-mappings");
        manager.workingDirectory = tempDirectory.toString();
    }

    @AfterEach
    public void afterEach() throws IOException {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String fieldName) {
        try {
            Field field = AgentMappingManager.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(manager);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public File getMappingsFile() {
        return new File(tempDirectory.toFile(), "agent_mappings.yaml");
    }

    @Nested
    public class PostConstruct {

        @Test
        public void noAgentMappingsAvailable() {
            manager.postConstruct();

            assertThat((Object) getField("mappingsFile")).isEqualTo(new File(tempDirectory.toFile(), "agent_mappings.yaml"));
            assertThat((List) getField("agentMappings")).isEmpty();
            assertThat((Object) getField("ymlMapper")).isNotNull();
            assertThat((Object) getField("agentMappingListType")).isNotNull();
        }

        @Test
        public void successfullyLoadAgentMappings() throws IOException {
            String mappingYaml = "- name: \"my-mapping\"\n" +
                    "  sources:\n" +
                    "  - \"/configs\"\n" +
                    "  attributes:\n" +
                    "    region: \"eu-west\"";
            Files.write(getMappingsFile().toPath(), mappingYaml.getBytes(StandardCharsets.UTF_8));

            manager.postConstruct();

            List<AgentMapping> agentMappings = getField("agentMappings");
            assertThat(agentMappings).hasSize(1);
            assertThat(agentMappings.get(0))
                    .extracting(AgentMapping::getName).isEqualTo("my-mapping");
            assertThat((Object) getField("mappingsFile")).isEqualTo(getMappingsFile());
            assertThat((Object) getField("ymlMapper")).isNotNull();
            assertThat((Object) getField("agentMappingListType")).isNotNull();
        }

        @Test
        public void agentMappingsAreBroken() throws IOException {
            String mappingYaml = "This is not a valid agent mapping!";
            File mappingsFile = new File(tempDirectory.toFile(), "agent_mappings.yaml");
            Files.write(mappingsFile.toPath(), mappingYaml.getBytes(StandardCharsets.UTF_8));

            manager.postConstruct();

            assertThat((Object) getField("mappingsFile")).isEqualTo(new File(tempDirectory.toFile(), "agent_mappings.yaml"));
            assertThat((List) getField("agentMappings")).isEmpty();
            assertThat((Object) getField("ymlMapper")).isNotNull();
            assertThat((Object) getField("agentMappingListType")).isNotNull();
        }
    }

    @Nested
    public class SetAgentMappings {

        @Test
        public void successfullySetMappings() throws IOException {
            File mappingsFile = getMappingsFile();
            AgentMapping mapping = AgentMapping.builder().name("my-mapping").attribute("attributeA", "valueA").source("sourceA").build();

            assertThat(mappingsFile.exists()).isFalse();

            manager.postConstruct();
            manager.setAgentMappings(Collections.singletonList(mapping));

            assertThat(mappingsFile.exists()).isTrue();
            assertThat(manager.getAgentMappings()).containsExactly(mapping);
            String mappingYaml = new String(Files.readAllBytes(mappingsFile.toPath()));
            assertThat(mappingYaml).isEqualTo("- name: \"my-mapping\"\n" +
                    "  sources:\n" +
                    "  - \"sourceA\"\n" +
                    "  attributes:\n" +
                    "    attributeA: \"valueA\"\n");
        }
    }
}