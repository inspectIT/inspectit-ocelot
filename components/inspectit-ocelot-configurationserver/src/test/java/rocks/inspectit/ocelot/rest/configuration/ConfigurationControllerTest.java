package rocks.inspectit.ocelot.rest.configuration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentstatus.AgentStatus;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.rest.agentstatus.AgentStatusController;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfigurationControllerTest {

    @InjectMocks
    ConfigurationController configurationController;

    @Mock
    AgentConfigurationManager agentConfigurationManager;

    @Nested
    public class FetchConfiguration {
        @InjectMocks
        AgentStatusController agentStatusController;

        @Mock
        AgentStatusManager agentStatusManager;

        @Test
        public void noLoggingEntryAdded() {
            when(agentConfigurationManager.getConfiguration(any())).thenReturn(null);

            Collection<AgentStatus> beforeRequest = agentStatusController.getAgentStatuses(null);
            ResponseEntity<String> output = configurationController.fetchConfiguration(null);
            Collection<AgentStatus> afterRequest = agentStatusController.getAgentStatuses(null);

            assertThat(output.getBody()).isEqualTo(null);
            assertThat(beforeRequest).isEqualTo(afterRequest);
        }
    }
}
