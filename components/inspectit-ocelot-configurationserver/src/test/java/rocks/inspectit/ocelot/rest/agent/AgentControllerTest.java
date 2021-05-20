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
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentControllerTest {

    @InjectMocks
    AgentController controller;

    @Mock
    AgentConfigurationManager configManager;

    @Mock
    AgentStatusManager statusManager;

    @Mock
    AgentCommandManager agentCommandManager;

    @Mock
    AgentCallbackManager agentCallbackManager;

    @Nested
    public class FetchConfiguration {

        @Test
        public void noMappingFound() {
            doReturn(null).when(configManager).getConfiguration(anyMap());

            HashMap<String, String> attributes = new HashMap<>();
            ResponseEntity<String> result = controller.fetchConfiguration(attributes, Collections.emptyMap());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(statusManager).notifyAgentConfigurationFetched(same(attributes), eq(Collections.emptyMap()), isNull());
        }

        @Test
        public void mappingFound() {
            AgentConfiguration config = AgentConfiguration.builder().configYaml("foo : bar").build();
            doReturn(config).when(configManager).getConfiguration(anyMap());

            HashMap<String, String> attributes = new HashMap<>();
            ResponseEntity<String> result = controller.fetchConfiguration(attributes, Collections.emptyMap());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo("foo : bar");
            verify(statusManager).notifyAgentConfigurationFetched(same(attributes), eq(Collections.emptyMap()), same(config));
        }

        @Test
        public void etagPresent() {
            AgentConfiguration config = AgentConfiguration.builder().configYaml("foo : bar").build();
            doReturn(config).when(configManager).getConfiguration(anyMap());

            ResponseEntity<String> firstResult = controller.fetchConfiguration(new HashMap<>(), Collections.emptyMap());
            ResponseEntity<String> secondResult = controller.fetchConfiguration(new HashMap<>(), Collections.emptyMap());

            assertThat(firstResult.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(firstResult.getBody()).isEqualTo("foo : bar");
            assertThat(secondResult.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(secondResult.getBody()).isEqualTo("foo : bar");
            assertThat(firstResult.getHeaders().getFirst("ETag")).isNotBlank()
                    .isEqualTo(secondResult.getHeaders().getFirst("ETag"));
        }
    }

    @Nested
    public class FetchNewCommand {

        @Test
        public void agentWithoutResponse() throws ExecutionException {
            HashMap<String, String> headers = new HashMap<>();
            String agentTestId = "test-id";
            headers.put("x-ocelot-agent-id", agentTestId);
            Command expectedCommand = null;
            doReturn(expectedCommand).when(agentCommandManager).getCommand(agentTestId);

            ResponseEntity<Command> result = controller.fetchNewCommand(headers, null);

            assertThat(result.getBody()).isEqualTo(expectedCommand);
            verify(agentCommandManager).getCommand(agentTestId);
        }

        @Test
        public void agentHasResponse() throws ExecutionException {
            HashMap<String, String> headers = new HashMap<>();
            String agentTestId = "test-id";
            headers.put("x-ocelot-agent-id", agentTestId);
            Command expectedCommand = new PingCommand();
            UUID mockID = expectedCommand.getCommandId();
            doReturn(expectedCommand).when(agentCommandManager).getCommand(agentTestId);
            CommandResponse mockResponse = mock(CommandResponse.class);

            doReturn(mockID).when(mockResponse).getCommandId();
            doNothing().when(agentCallbackManager).handleCommandResponse(expectedCommand.getCommandId(), mockResponse);

            ResponseEntity<Command> result = controller.fetchNewCommand(headers, mockResponse);

            assertThat(result.getBody()).isEqualTo(expectedCommand);
            verify(agentCommandManager).getCommand(agentTestId);
            verify(agentCallbackManager).handleCommandResponse(mockID, mockResponse);
        }
    }
}
