package rocks.inspectit.ocelot.agentcommunication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentCommandManagerTest {

    @InjectMocks
    AgentCommandManager agentCommandManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitServerSettings configuration;

    @BeforeEach
    public void beforeEach() {
        when(configuration.getAgentCommand().getCommandTimeout()).thenReturn(Duration.ofSeconds(5));
        when(configuration.getAgentCommand().getCommandQueueSize()).thenReturn(10);

        agentCommandManager.postConstruct();
    }

    @Nested
    class AddCommand {

        @Test
        public void addCommandNewAgent() throws ExecutionException {
            Command mockAgentCommand = mock(Command.class);
            String agentId = "test-agent";

            agentCommandManager.addCommand(agentId, mockAgentCommand);

            Map<String, BlockingQueue<Command>> result = agentCommandManager.agentCommandCache.asMap();
            assertThat(result).containsOnlyKeys(agentId);
            BlockingQueue<Command> commandList = result.get(agentId);
            assertThat(commandList).containsExactly(mockAgentCommand);
        }

        @Test
        public void throwsExeptionOnNullId() throws ExecutionException {
            Command mockAgentCommand = mock(Command.class);

            try {
                agentCommandManager.addCommand(null, mockAgentCommand);
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("Agent id may never be null!");
            }

        }

        @Test
        public void ignoresNullCommand() throws ExecutionException {
            String agentId = "test-agent";

            agentCommandManager.addCommand(agentId, null);

            Map<String, BlockingQueue<Command>> result = agentCommandManager.agentCommandCache.asMap();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetCommand {

        @Test
        public void noCommandPresent() {
            String agentId = "test-agent";

            Command result = agentCommandManager.getCommand(agentId, false);

            assertThat(result).isEqualTo(null);
        }

        @Test
        public void commandPresent() {
            Command mockAgentCommand = mock(Command.class);
            String agentId = "test-agent";
            BlockingQueue<Command> list = new LinkedBlockingQueue<>();
            list.add(mockAgentCommand);
            agentCommandManager.agentCommandCache.put(agentId, list);

            Command result = agentCommandManager.getCommand(agentId, false);
            // simulate periodic command fetch that invalidates the command-cache
            agentCommandManager.getCommand(agentId, false);

            assertThat(result).isEqualTo(mockAgentCommand);
            assertThat(agentCommandManager.agentCommandCache.asMap()).isEmpty();
        }

        @Test
        public void commandOfDifferentAgentPresent() {
            Command mockAgentCommand = mock(Command.class);
            String agentId = "not-the-test-agent";
            BlockingQueue<Command> list = new LinkedBlockingQueue<>();
            list.add(mockAgentCommand);
            agentCommandManager.agentCommandCache.put(agentId, list);

            Command result = agentCommandManager.getCommand("test-agent", false);

            assertThat(result).isEqualTo(null);
            assertThat(agentCommandManager.agentCommandCache.asMap()).containsOnlyKeys(agentId);
        }
    }

}
