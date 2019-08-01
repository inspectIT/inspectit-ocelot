package rocks.inspectit.ocelot.mappings;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentMappingManagerTest {

    private static AgentMappingSerializer mapperUtils;

    @InjectMocks
    AgentMappingManager manager;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Spy
    AgentMappingSerializer objectMapperUtils = new AgentMappingSerializer();

    Path tempDirectory;

    @BeforeAll
    public static void beforeAll() {
        mapperUtils = new AgentMappingSerializer();
        mapperUtils.postConstruct();
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        tempDirectory = Files.createTempDirectory("agent-mappings");

        InspectitServerSettings conf = new InspectitServerSettings();
        conf.setWorkingDirectory(tempDirectory.toString());
        manager.config = conf;

        objectMapperUtils.postConstruct();
        verify(objectMapperUtils).postConstruct();
    }

    void removeDefaultMapping() {
        manager.agentMappings.remove(AgentMappingManager.DEFAULT_MAPPING);
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
            assertThat((List) getField("agentMappings")).containsExactly(AgentMappingManager.DEFAULT_MAPPING);
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
        }

        @Test
        public void agentMappingsAreBroken() throws IOException {
            String mappingYaml = "This is not a valid agent mapping!";
            File mappingsFile = new File(tempDirectory.toFile(), "agent_mappings.yaml");
            Files.write(mappingsFile.toPath(), mappingYaml.getBytes(StandardCharsets.UTF_8));

            manager.postConstruct();

            assertThat((Object) getField("mappingsFile")).isEqualTo(new File(tempDirectory.toFile(), "agent_mappings.yaml"));
            assertThat((List) getField("agentMappings")).isEmpty();
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
            List<AgentMapping> resultMappings = mapperUtils.readAgentMappings(mappingsFile);
            assertThat(resultMappings).hasSize(1);
            assertThat(resultMappings.get(0)).isEqualTo(mapping);
            verify(objectMapperUtils).writeAgentMappings(any(), eq(mappingsFile));
            verifyNoMoreInteractions(objectMapperUtils);

            verify(eventPublisher).publishEvent(any(AgentMappingsChangedEvent.class));
        }

        @Test
        public void writingMappingsFileFails() throws IOException {
            AgentMapping mapping = AgentMapping.builder().name("mapping").build();
            doThrow(IOException.class).when(objectMapperUtils).writeAgentMappings(any(), any());

            manager.postConstruct();
            removeDefaultMapping();

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> manager.setAgentMappings(Collections.singletonList(mapping)));

            assertThat(manager.getAgentMappings()).isEmpty();
        }

        @Test
        public void writingMappingsFileFailsDoNothing() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("mappingA").build();
            AgentMapping mappingB = AgentMapping.builder().name("mappingB").build();
            AgentMapping mappingC = AgentMapping.builder().name("mappingC").build();
            doNothing().doThrow(IOException.class).when(objectMapperUtils).writeAgentMappings(any(), any());

            manager.postConstruct();
            manager.setAgentMappings(Collections.singletonList(mappingA));

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> manager.setAgentMappings(Arrays.asList(mappingB, mappingC)));

            assertThat(manager.getAgentMappings()).hasSize(1);
            assertThat(manager.getAgentMappings().get(0)).isEqualTo(mappingA);
        }

        @Test
        public void setNull() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.setAgentMappings(null))
                    .withMessage("The agent mappings should not be null.");

            verifyZeroInteractions(objectMapperUtils);
        }
    }

    @Nested
    public class GetAgentMapping {

        @Test
        public void getAgentMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB));
            verify(objectMapperUtils).writeAgentMappings(any(), any());

            Optional<AgentMapping> result = manager.getAgentMapping("second");

            assertThat(result).isNotEmpty();
            assertThat(result).contains(mappingB);
            verifyZeroInteractions(objectMapperUtils);
        }

        @Test
        public void noMappingFound() throws IOException {
            AgentMapping mapping = AgentMapping.builder().name("first").build();

            manager.postConstruct();
            manager.setAgentMappings(Collections.singletonList(mapping));
            verify(objectMapperUtils).writeAgentMappings(any(), any());

            Optional<AgentMapping> result = manager.getAgentMapping("not-existing");

            assertThat(result).isEmpty();
            verifyZeroInteractions(objectMapperUtils);
        }

        @Test
        public void getNullName() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.getAgentMapping(null))
                    .withMessage("The mapping name should not be empty or null.");

            verifyZeroInteractions(objectMapperUtils);
        }

        @Test
        public void getEmptyName() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.getAgentMapping(""))
                    .withMessage("The mapping name should not be empty or null.");

            verifyZeroInteractions(objectMapperUtils);
        }
    }

    @Nested
    public class DeleteAgentMapping {

        @Test
        public void successfullyDeleteMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB));

            boolean result = manager.deleteAgentMapping("first");

            assertThat(result).isTrue();
            assertThat(manager.getAgentMappings()).containsExactly(mappingB);
            verify(objectMapperUtils, times(2)).writeAgentMappings(any(), any());
            verifyNoMoreInteractions(objectMapperUtils);

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }

        @Test
        public void mappingDoesNotExist() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();

            manager.postConstruct();
            manager.setAgentMappings(Collections.singletonList(mappingA));
            verify(objectMapperUtils).writeAgentMappings(any(), any());

            boolean result = manager.deleteAgentMapping("not-existing");

            assertThat(result).isFalse();
            verifyZeroInteractions(objectMapperUtils);
        }

        @Test
        public void writingFileFails() throws IOException {
            doNothing().doThrow(IOException.class).when(objectMapperUtils).writeAgentMappings(any(), any());
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB));

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> manager.deleteAgentMapping("first"));

            verify(objectMapperUtils, times(2)).writeAgentMappings(any(), any());
            verifyNoMoreInteractions(objectMapperUtils);
        }

        @Test
        public void deleteNullMapping() {
            manager.postConstruct();

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.deleteAgentMapping(null))
                    .withMessage("The mapping name should not be empty or null.");

            verifyZeroInteractions(objectMapperUtils);
        }
    }

    @Nested
    public class AddAgentMapping {

        @Test
        public void addingAgentMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();

            manager.postConstruct();
            removeDefaultMapping();

            manager.addAgentMapping(mappingA);

            assertThat(manager.getAgentMappings()).containsExactly(mappingA);
            verify(objectMapperUtils).writeAgentMappings(any(), any());
            verifyNoMoreInteractions(objectMapperUtils);

            verify(eventPublisher).publishEvent(any(AgentMappingsChangedEvent.class));
        }

        @Test
        public void addingAgentMappingToExisting() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            manager.postConstruct();
            removeDefaultMapping();

            manager.addAgentMapping(mappingA);
            manager.addAgentMapping(mappingB);

            assertThat(manager.getAgentMappings()).containsExactly(mappingB, mappingA);
            verify(objectMapperUtils, times(2)).writeAgentMappings(any(), any());
            verifyNoMoreInteractions(objectMapperUtils);

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }

        @Test
        public void addNullMapping() {
            manager.postConstruct();

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.addAgentMapping(null))
                    .withMessage("The agent mapping should not be null.");

            verifyZeroInteractions(objectMapperUtils);
        }

        @Test
        public void addingAgentMappingWithNullName() {
            AgentMapping mappingA = AgentMapping.builder().name(null).build();

            manager.postConstruct();

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.addAgentMapping(mappingA))
                    .withMessage("The agent mapping's name should not be null or empty.");

            verifyZeroInteractions(objectMapperUtils);
        }

        @Test
        public void addingAgentMappingWithEmptyName() {
            AgentMapping mappingA = AgentMapping.builder().name("").build();

            manager.postConstruct();

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.addAgentMapping(mappingA))
                    .withMessage("The agent mapping's name should not be null or empty.");

            verifyZeroInteractions(objectMapperUtils);
        }

        @Test
        public void updateMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("mapping").build();

            manager.postConstruct();
            removeDefaultMapping();

            manager.addAgentMapping(mappingA);

            assertThat(manager.getAgentMappings()).containsExactly(mappingA);
            AgentMapping storedMapping = manager.getAgentMapping("mapping").get();
            assertThat(storedMapping.getSources()).isEmpty();

            mappingA = AgentMapping.builder().name("mapping").source("/newSource").build();
            manager.addAgentMapping(mappingA);

            assertThat(manager.getAgentMappings()).containsExactly(mappingA);
            storedMapping = manager.getAgentMapping("mapping").get();
            assertThat(storedMapping.getSources()).contains("/newSource");

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }
    }

    @Nested
    public class AddAgentMappingBefore {

        @Test
        public void successfullyAddMappingBefore() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB));

            manager.addAgentMappingBefore(mappingC, "second");

            assertThat(manager.getAgentMappings()).containsExactly(mappingA, mappingC, mappingB);
            verify(objectMapperUtils, times(2)).writeAgentMappings(any(), any());
            verifyNoMoreInteractions(objectMapperUtils);

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }

        @Test
        public void addBeforeFirst() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB));

            manager.addAgentMappingBefore(mappingC, "first");

            assertThat(manager.getAgentMappings()).containsExactly(mappingC, mappingA, mappingB);

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }


        @Test
        public void addBeforeItself() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB, mappingC));

            manager.addAgentMappingBefore(mappingB, "second");

            assertThat(manager.getAgentMappings()).containsExactly(mappingA, mappingB, mappingC);

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }

        @Test
        public void moveExistingMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB, mappingC));
            assertThat(manager.getAgentMappings()).containsExactly(mappingA, mappingB, mappingC);

            manager.addAgentMappingBefore(mappingC, "first");

            assertThat(manager.getAgentMappings()).containsExactly(mappingC, mappingA, mappingB);

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }

        @Test
        public void targetNotExists() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            manager.postConstruct();
            manager.setAgentMappings(Collections.singletonList(mappingA));

            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> manager.addAgentMappingBefore(mappingB, "not-existing"))
                    .withMessage("The agent mapping has not been added because the mapping 'not-existing' does not exists, thus, cannot be added before it.");

            assertThat(manager.getAgentMappings()).containsExactly(mappingA);
        }
    }

    @Nested
    public class AddAgentMappingAfter {

        @Test
        public void successfullyAddMappingAfter() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB));

            manager.addAgentMappingAfter(mappingC, "second");

            assertThat(manager.getAgentMappings()).containsExactly(mappingA, mappingB, mappingC);
            verify(objectMapperUtils, times(2)).writeAgentMappings(any(), any());
            verifyNoMoreInteractions(objectMapperUtils);

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }

        @Test
        public void addAfterItself() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB, mappingC));

            manager.addAgentMappingAfter(mappingB, "second");

            assertThat(manager.getAgentMappings()).containsExactly(mappingA, mappingB, mappingC);

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }

        @Test
        public void moveExistingMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            manager.postConstruct();
            manager.setAgentMappings(Arrays.asList(mappingA, mappingB, mappingC));
            assertThat(manager.getAgentMappings()).containsExactly(mappingA, mappingB, mappingC);

            manager.addAgentMappingAfter(mappingC, "first");

            assertThat(manager.getAgentMappings()).containsExactly(mappingA, mappingC, mappingB);

            verify(eventPublisher, times(2)).publishEvent(any(AgentMappingsChangedEvent.class));
        }


        @Test
        public void targetNotExists() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            manager.postConstruct();
            manager.setAgentMappings(Collections.singletonList(mappingA));

            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> manager.addAgentMappingAfter(mappingB, "not-existing"))
                    .withMessage("The agent mapping has not been added because the mapping 'not-existing' does not exists, thus, cannot be added after it.");

            assertThat(manager.getAgentMappings()).containsExactly(mappingA);
        }
    }
}