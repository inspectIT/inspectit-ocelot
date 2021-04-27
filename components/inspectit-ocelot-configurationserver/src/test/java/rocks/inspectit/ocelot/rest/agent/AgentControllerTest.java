package rocks.inspectit.ocelot.rest.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.agentcommand.AgentCallbackManager;
import rocks.inspectit.ocelot.agentcommand.AgentCommand;
import rocks.inspectit.ocelot.agentcommand.AgentCommandManager;
import rocks.inspectit.ocelot.agentcommand.AgentResponse;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;

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
        public void noMappingFound() throws Exception {
            doReturn(null).when(configManager).getConfiguration(anyMap());

            HashMap<String, String> attributes = new HashMap<>();
            ResponseEntity<String> result = controller.fetchConfiguration(attributes, Collections.emptyMap());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(statusManager).notifyAgentConfigurationFetched(same(attributes), eq(Collections.emptyMap()), isNull());
        }

        @Test
        public void mappingFound() throws Exception {
            AgentConfiguration config = AgentConfiguration.builder().configYaml("foo : bar").build();
            doReturn(config).when(configManager).getConfiguration(anyMap());

            HashMap<String, String> attributes = new HashMap<>();
            ResponseEntity<String> result = controller.fetchConfiguration(attributes, Collections.emptyMap());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo("foo : bar");
            verify(statusManager).notifyAgentConfigurationFetched(same(attributes), eq(Collections.emptyMap()), same(config));
        }


        @Test
        public void etagPresent() throws Exception {
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
        public void noIdInHeader() throws JsonProcessingException, ExecutionException {
            AgentCommand expectedCommand = AgentCommand.getEmptyCommand();
            String expectedJson = new ObjectMapper().writer()
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(expectedCommand);

            ResponseEntity<String> result = controller.fetchNewCommand(new HashMap<>(), null);

            assertThat(result.getBody()).isEqualTo(expectedJson);
        }

        @Test
        public void commandAvailable() throws JsonProcessingException, ExecutionException {
            HashMap<String, String> headers = new HashMap<>();
            String agentTestId = "test-id";
            headers.put("x-ocelot-agent-id", agentTestId);
            AgentCommand testCommand = AgentCommand.getEmptyCommand();
            String expectedJson = new ObjectMapper().writer()
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(testCommand);
            doReturn(testCommand).when(agentCommandManager).getCommand(agentTestId);

            ResponseEntity<String> result = controller.fetchNewCommand(headers, null);

            assertThat(result.getBody()).isEqualTo(expectedJson);
            verify(agentCommandManager).getCommand(agentTestId);
        }

        @Test
        public void hasResponse() throws JsonProcessingException, ExecutionException {
            HashMap<String, String> headers = new HashMap<>();
            String agentTestId = "test-id";
            headers.put("x-ocelot-agent-id", agentTestId);
            AgentCommand testCommand = new AgentCommand(null, agentTestId, null, null);
            String expectedJson = new ObjectMapper().writer()
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(testCommand);
            doReturn(testCommand).when(agentCommandManager).getCommand(agentTestId);
            AgentResponse mockResponse = mock(AgentResponse.class);
            UUID mockID = UUID.randomUUID();
            doReturn(mockID).when(mockResponse).getCommandId();
            doNothing().when(agentCallbackManager).runNextCommandWithId(agentTestId, mockID);

            ResponseEntity<String> result = controller.fetchNewCommand(headers, mockResponse);

            assertThat(result.getBody()).isEqualTo(expectedJson);
            verify(agentCommandManager).getCommand(agentTestId);
            verify(agentCallbackManager).runNextCommandWithId(agentTestId, mockID);
        }
    }
}
