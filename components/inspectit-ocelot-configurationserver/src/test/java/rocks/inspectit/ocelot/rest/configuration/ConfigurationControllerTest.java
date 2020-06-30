package rocks.inspectit.ocelot.rest.configuration;

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
import rocks.inspectit.ocelot.agentstatus.AgentStatus;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.rest.agentstatus.AgentStatusController;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfigurationControllerTest {

    @InjectMocks
    ConfigurationController configurationController;

    @Mock
    AgentConfigurationManager agentConfigurationManager;

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
}
