package rocks.inspectit.ocelot.rest.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import inspectit.ocelot.configdocsgenerator.ConfigDocsGenerator;
import inspectit.ocelot.configdocsgenerator.model.ConfigDocumentation;
import org.assertj.core.api.AssertionsForClassTypes;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentconfiguration.ObjectStructureMerger;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;
import rocks.inspectit.ocelot.rest.file.DefaultConfigController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConfigurationControllerTest {

    @InjectMocks
    ConfigurationController configurationController;

    @Mock
    AgentConfigurationManager agentConfigurationManager;

    @Mock
    FileManager fileManager;

    @Mock
    private AgentMappingManager mappingManager;

    @Mock
    private ConfigDocsGenerator configDocsGenerator;

    @Mock
    private DefaultConfigController defaultConfigController;

    @Mock
    private Yaml yaml;

    @Nested
    public class FetchConfiguration {

        @Test
        public void returningConfiguration() {
            AgentConfiguration configuration = AgentConfiguration.builder().configYaml("yaml").build();
            when(agentConfigurationManager.getConfiguration(any())).thenReturn(configuration);

            ResponseEntity<String> output = configurationController.fetchConfiguration(null);

            assertThat(output.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(output.getBody()).isEqualTo("yaml");
        }

        @Test
        public void noConfigurationAvailable() {
            when(agentConfigurationManager.getConfiguration(any())).thenReturn(null);

            ResponseEntity<String> output = configurationController.fetchConfiguration(null);

            assertThat(output.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    public class ReloadConfiguration {

        @Test
        public void initiatesCommit() throws GitAPIException {
            configurationController.reloadConfiguration();

            verify(fileManager).commitWorkingDirectory();
        }
    }

    @Nested
    public class GetConfigDocumentationTest {

        @Test
        void withDefaultConfig() throws IOException {

            final String mappingName = "name";
            AgentMapping agentMapping = AgentMapping.builder().build();

            final String configYaml = "yaml";
            AgentConfiguration agentConfiguration = AgentConfiguration.builder().configYaml(configYaml).build();

            final Map<String, String> defaultYamls = new HashMap<>();
            final String defaultYamlContent = "defaultYaml";
            defaultYamls.put("firstYaml", defaultYamlContent);

            final Map<String, String> configYamlMap = new HashMap<>();
            configYamlMap.put("entry", "value");

            final Map<String, String> defaultConfigYamlMap = new HashMap<>();
            defaultConfigYamlMap.put("defaultEntry", "defaultValue");

            final Object combinedYamls = ObjectStructureMerger.merge(configYamlMap, defaultConfigYamlMap);
            final String combinedYamlString = "bothYamls";

            ConfigDocumentation configDocumentationMock = mock(ConfigDocumentation.class);

            when(mappingManager.getAgentMapping(mappingName)).thenReturn(Optional.of(agentMapping));
            when(agentConfigurationManager.getConfigurationForMapping(agentMapping)).thenReturn(agentConfiguration);
            when(defaultConfigController.getDefaultConfigContent()).thenReturn(defaultYamls);
            when(yaml.load(anyString())).thenReturn(configYamlMap, defaultConfigYamlMap);
            when(yaml.dump(eq(combinedYamls))).thenReturn(combinedYamlString);
            when(configDocsGenerator.generateConfigDocs(combinedYamlString)).thenReturn(configDocumentationMock);

            ResponseEntity<Object> result = configurationController.getConfigDocumentation(mappingName, true);

            verify(mappingManager).getAgentMapping(eq(mappingName));
            verifyNoMoreInteractions(mappingManager);
            verify(agentConfigurationManager).getConfigurationForMapping(eq(agentMapping));
            verifyNoMoreInteractions(agentConfigurationManager);
            verify(defaultConfigController).getDefaultConfigContent();
            verifyNoMoreInteractions(defaultConfigController);
            verify(yaml).load(eq(defaultYamlContent));
            verify(yaml).load(eq(configYaml));
            verify(yaml).dump(eq(combinedYamls));
            verifyNoMoreInteractions(yaml);
            verify(configDocsGenerator).generateConfigDocs(eq(combinedYamlString));
            verifyNoMoreInteractions(configDocsGenerator);

            AssertionsForClassTypes.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            AssertionsForClassTypes.assertThat(result.getBody()).isSameAs(configDocumentationMock);
        }

        @Test
        void errorWhenGettingDefaultConfig() throws IOException {

            final String mappingName = "name";
            AgentMapping agentMapping = AgentMapping.builder().build();

            final String configYaml = "yaml";
            AgentConfiguration agentConfiguration = AgentConfiguration.builder().configYaml(configYaml).build();

            IOException exception = new IOException();

            ConfigDocumentation configDocumentationMock = mock(ConfigDocumentation.class);

            when(mappingManager.getAgentMapping(mappingName)).thenReturn(Optional.of(agentMapping));
            when(agentConfigurationManager.getConfigurationForMapping(agentMapping)).thenReturn(agentConfiguration);
            when(defaultConfigController.getDefaultConfigContent()).thenThrow(exception);

            ResponseEntity<Object> result = configurationController.getConfigDocumentation(mappingName, true);

            verify(mappingManager).getAgentMapping(eq(mappingName));
            verifyNoMoreInteractions(mappingManager);
            verify(agentConfigurationManager).getConfigurationForMapping(eq(agentMapping));
            verifyNoMoreInteractions(agentConfigurationManager);
            verify(defaultConfigController).getDefaultConfigContent();
            verifyNoMoreInteractions(defaultConfigController);
            verifyNoInteractions(yaml);
            verifyNoInteractions(configDocsGenerator);

            AssertionsForClassTypes.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            AssertionsForClassTypes.assertThat(result.getBody()).isEqualTo(
                    String.format("Config Documentation for given AgentMapping '%s' could not be generated due to the following error: %s.", mappingName, exception.getMessage()));
        }

        @Test
        void withoutDefaultConfig() throws JsonProcessingException {

            final String mappingName = "name";
            AgentMapping agentMapping = AgentMapping.builder().build();

            final String configYaml = "yaml";
            AgentConfiguration agentConfiguration = AgentConfiguration.builder().configYaml(configYaml).build();

            ConfigDocumentation configDocumentationMock = mock(ConfigDocumentation.class);

            when(mappingManager.getAgentMapping(mappingName)).thenReturn(Optional.of(agentMapping));
            when(agentConfigurationManager.getConfigurationForMapping(agentMapping)).thenReturn(agentConfiguration);
            when(configDocsGenerator.generateConfigDocs(configYaml)).thenReturn(configDocumentationMock);

            ResponseEntity<Object> result = configurationController.getConfigDocumentation(mappingName, false);

            verifyNoInteractions(defaultConfigController);
            verifyNoInteractions(yaml);
            verify(mappingManager).getAgentMapping(eq(mappingName));
            verifyNoMoreInteractions(mappingManager);
            verify(agentConfigurationManager).getConfigurationForMapping(eq(agentMapping));
            verifyNoMoreInteractions(agentConfigurationManager);
            verify(configDocsGenerator).generateConfigDocs(eq(configYaml));
            verifyNoMoreInteractions(configDocsGenerator);

            AssertionsForClassTypes.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            AssertionsForClassTypes.assertThat(result.getBody()).isSameAs(configDocumentationMock);
        }

        @Test
        void agentMappingNotFound() {

            final String mappingName = "name";

            when(mappingManager.getAgentMapping(mappingName)).thenReturn(Optional.empty());

            ResponseEntity<Object> result = configurationController.getConfigDocumentation(mappingName, false);

            verify(mappingManager).getAgentMapping(eq(mappingName));
            verifyNoMoreInteractions(mappingManager);

            AssertionsForClassTypes.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            AssertionsForClassTypes.assertThat(result.getBody())
                    .isEqualTo(String.format("No AgentMapping found with the name '%s'.", mappingName));

        }

        @Test
        void invalidYaml() throws JsonProcessingException {

            final String mappingName = "name";
            AgentMapping agentMapping = AgentMapping.builder().build();

            final String configYaml = "yaml";
            AgentConfiguration agentConfiguration = AgentConfiguration.builder().configYaml(configYaml).build();

            JsonProcessingException exception = mock(JsonProcessingException.class);
            final String errorMessage = "JsonProcessingException: Yaml could not be processed.";

            when(mappingManager.getAgentMapping(mappingName)).thenReturn(Optional.of(agentMapping));
            when(agentConfigurationManager.getConfigurationForMapping(agentMapping)).thenReturn(agentConfiguration);
            when(configDocsGenerator.generateConfigDocs(configYaml)).thenThrow(exception);
            when(exception.getMessage()).thenReturn(errorMessage);

            ResponseEntity<Object> result = configurationController.getConfigDocumentation(mappingName, false);

            verifyNoInteractions(defaultConfigController);
            verifyNoInteractions(yaml);
            verify(mappingManager).getAgentMapping(eq(mappingName));
            verifyNoMoreInteractions(mappingManager);
            verify(agentConfigurationManager).getConfigurationForMapping(eq(agentMapping));
            verifyNoMoreInteractions(agentConfigurationManager);
            verify(configDocsGenerator).generateConfigDocs(eq(configYaml));
            verifyNoMoreInteractions(configDocsGenerator);

            AssertionsForClassTypes.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            AssertionsForClassTypes.assertThat(result.getBody()).isEqualTo(
                    String.format("Config Documentation for given AgentMapping '%s' could not be generated due to the following error: %s.", mappingName, exception.getMessage()));
        }
    }
}