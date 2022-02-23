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
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

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
        void getConfigDocumentation() throws JsonProcessingException {

            String mappingName = "name";
            AgentMapping agentMapping = AgentMapping.builder().build();

            String configYaml = "yaml";
            AgentConfiguration agentConfiguration = AgentConfiguration.builder().configYaml(configYaml).build();

            ConfigDocumentation configDocumentationMock = mock(ConfigDocumentation.class);

            when(mappingManager.getAgentMapping(mappingName)).thenReturn(Optional.of(agentMapping));
            when(agentConfigurationManager.getConfigurationForMapping(agentMapping)).thenReturn(agentConfiguration);
            when(configDocsGenerator.generateConfigDocs(configYaml)).thenReturn(configDocumentationMock);

            ResponseEntity<Object> result = configurationController.getConfigDocumentation(mappingName);

            verify(mappingManager).getAgentMapping(eq(mappingName));
            verifyNoMoreInteractions(mappingManager);
            verify(agentConfigurationManager).getConfigurationForMapping(eq(agentMapping));
            verifyNoMoreInteractions(agentConfigurationManager);
            verify(configDocsGenerator).generateConfigDocs(eq(configYaml));
            verifyNoMoreInteractions(configDocsGenerator);

            AssertionsForClassTypes.assertThat(result.getBody()).isSameAs(configDocumentationMock);
        }

        @Test
        void getConfigDocumentationMappingNotFound() {

            String mappingName = "name";

            when(mappingManager.getAgentMapping(mappingName)).thenReturn(Optional.empty());

            ResponseEntity<Object> result = configurationController.getConfigDocumentation(mappingName);

            verify(mappingManager).getAgentMapping(eq(mappingName));
            verifyNoMoreInteractions(mappingManager);

            AssertionsForClassTypes.assertThat(result.getStatusCodeValue()).isEqualTo(404);
            AssertionsForClassTypes.assertThat(result.getBody())
                    .isEqualTo(String.format("No AgentMapping found with the name %s.", mappingName));

        }
    }
}
