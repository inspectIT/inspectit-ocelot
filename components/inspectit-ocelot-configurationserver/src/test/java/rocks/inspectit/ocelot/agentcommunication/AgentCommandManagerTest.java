package rocks.inspectit.ocelot.agentcommunication;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.command.Command;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AgentCommandManagerTest {

    @InjectMocks
    AgentCommandManager agentCommandManager;

    @Nested
    class AddCommand {

        @Test
        public void addCommandNewAgent() throws ExecutionException {
            Command mockAgentCommand = mock(Command.class);
            String agentId = "test-agent";

            agentCommandManager.addCommand(agentId, mockAgentCommand);

            Map<String, LinkedList<Command>> result = agentCommandManager.agentCommandCache.asMap();
            assertThat(result).containsOnlyKeys(agentId);
            List<Command> commandList = result.get(agentId);
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

            Map<String, LinkedList<Command>> result = agentCommandManager.agentCommandCache.asMap();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetCommand {

        @Test
        public void noCommandPresent() throws ExecutionException {
            String agentId = "test-agent";

            Command result = agentCommandManager.getCommand(agentId);

            assertThat(result).isEqualTo(null);
        }

        @Test
        public void commandPresent() throws ExecutionException {
            Command mockAgentCommand = mock(Command.class);
            String agentId = "test-agent";
            LinkedList<Command> list = new LinkedList<>();
            list.add(mockAgentCommand);
            agentCommandManager.agentCommandCache.put(agentId, list);

            Command result = agentCommandManager.getCommand(agentId);

            assertThat(result).isEqualTo(mockAgentCommand);
            assertThat(agentCommandManager.agentCommandCache.asMap()).isEmpty();
        }

        @Test
        public void commandOfDifferentAgentPresent() throws ExecutionException {
            Command mockAgentCommand = mock(Command.class);
            String agentId = "not-the-test-agent";
            LinkedList<Command> list = new LinkedList<>();
            list.add(mockAgentCommand);
            agentCommandManager.agentCommandCache.put(agentId, list);

            Command result = agentCommandManager.getCommand("test-agent");

            assertThat(result).isEqualTo(null);
            assertThat(agentCommandManager.agentCommandCache.asMap()).containsOnlyKeys(agentId);
        }
    }



}
