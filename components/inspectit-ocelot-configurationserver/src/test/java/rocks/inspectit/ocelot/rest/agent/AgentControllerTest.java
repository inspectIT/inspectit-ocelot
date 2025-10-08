package rocks.inspectit.ocelot.rest.agent;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.agentcommunication.AgentCallbackManager;
import rocks.inspectit.ocelot.agentcommunication.AgentCommandManager;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @InjectMocks
    AgentController controller;

    @Mock
    AgentConfigurationManager configManager;

    @Mock
    AgentConfiguration agentConfiguration;

    @Mock
    AgentStatusManager statusManager;

    @Mock
    AgentCommandManager agentCommandManager;

    @Mock
    AgentCallbackManager agentCallbackManager;

    @Nested
    class FetchConfiguration {

        String srcYaml = "foo : bar";

        @Test
        void noMappingFound() {
            doReturn(null).when(configManager).getConfiguration(anyMap());

            HashMap<String, String> attributes = new HashMap<>();
            ResponseEntity<String> result = controller.fetchConfiguration(attributes, Collections.emptyMap());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(statusManager).notifyAgentConfigurationFetched(same(attributes), eq(Collections.emptyMap()), isNull());
        }

        @Test
        void mappingFound() {
            doReturn(agentConfiguration).when(configManager).getConfiguration(anyMap());
            doReturn(srcYaml).when(agentConfiguration).getConfigYaml();

            HashMap<String, String> attributes = new HashMap<>();
            ResponseEntity<String> result = controller.fetchConfiguration(attributes, Collections.emptyMap());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(srcYaml);
            verify(statusManager).notifyAgentConfigurationFetched(same(attributes), eq(Collections.emptyMap()), same(agentConfiguration));
        }

        @Test
        void etagPresent() {
            String hash = "1234";
            doReturn(agentConfiguration).when(configManager).getConfiguration(anyMap());
            doReturn(srcYaml).when(agentConfiguration).getConfigYaml();
            doReturn(hash).when(agentConfiguration).getHash();

            ResponseEntity<String> firstResult = controller.fetchConfiguration(new HashMap<>(), Collections.emptyMap());
            ResponseEntity<String> secondResult = controller.fetchConfiguration(new HashMap<>(), Collections.emptyMap());

            assertThat(firstResult.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(firstResult.getBody()).isEqualTo(srcYaml);
            assertThat(secondResult.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(secondResult.getBody()).isEqualTo(srcYaml);
            assertThat(firstResult.getHeaders().getFirst("ETag")).isNotBlank()
                    .isEqualTo(secondResult.getHeaders().getFirst("ETag"));
        }
    }

    @Nested
    class FetchCommand {

        @Test
        void agentWithoutResponse() {
            HashMap<String, String> headers = new HashMap<>();
            String agentTestId = "test-id";
            headers.put("x-ocelot-agent-id", agentTestId);
            Command expectedCommand = null;
            doReturn(expectedCommand).when(agentCommandManager).getCommand(agentTestId, false);

            ResponseEntity<Command> result = controller.fetchCommand(headers, false, null);

            assertThat(result.getBody()).isEqualTo(expectedCommand);
            verify(agentCommandManager).getCommand(agentTestId, false);
        }

        @Test
        void agentHasResponse() {
            HashMap<String, String> headers = new HashMap<>();
            String agentTestId = "test-id";
            headers.put("x-ocelot-agent-id", agentTestId);
            Command expectedCommand = new PingCommand();
            UUID mockID = expectedCommand.getCommandId();
            doReturn(expectedCommand).when(agentCommandManager).getCommand(agentTestId, false);
            CommandResponse mockResponse = mock(CommandResponse.class);

            doReturn(mockID).when(mockResponse).getCommandId();
            doNothing().when(agentCallbackManager).handleCommandResponse(expectedCommand.getCommandId(), mockResponse);

            ResponseEntity<Command> result = controller.fetchCommand(headers, false, mockResponse);

            assertThat(result.getBody()).isEqualTo(expectedCommand);
            verify(agentCommandManager).getCommand(agentTestId, false);
            verify(agentCallbackManager).handleCommandResponse(mockID, mockResponse);
        }
    }
}
