package rocks.inspectit.ocelot.agentcommunication;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.AgentCommand;

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
            AgentCommand mockAgentCommand = mock(AgentCommand.class);
            String agentId = "test-agent";

            agentCommandManager.addCommand(agentId, mockAgentCommand);

            Map<String, LinkedList<AgentCommand>> result = agentCommandManager.agentCommandCache.asMap();
            assertThat(result).containsOnlyKeys(agentId);
            List<AgentCommand> commandList = result.get(agentId);
            assertThat(commandList).containsExactly(mockAgentCommand);
        }

        @Test
        public void ignoresNullId() throws ExecutionException {
            AgentCommand mockAgentCommand = mock(AgentCommand.class);

            agentCommandManager.addCommand(null, mockAgentCommand);

            Map<String, LinkedList<AgentCommand>> result = agentCommandManager.agentCommandCache.asMap();
            assertThat(result).isEmpty();
        }

        @Test
        public void ignoresNullCommand() throws ExecutionException {
            String agentId = "test-agent";

            agentCommandManager.addCommand(agentId, null);

            Map<String, LinkedList<AgentCommand>> result = agentCommandManager.agentCommandCache.asMap();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetCommand {

        @Test
        public void noCommandPresent() throws ExecutionException {
            String agentId = "test-agent";

            AgentCommand result = agentCommandManager.getCommand(agentId);

            assertThat(result).isEqualTo(AgentCommand.getEmptyCommand());
        }

        @Test
        public void commandPresent() throws ExecutionException {
            AgentCommand mockAgentCommand = mock(AgentCommand.class);
            String agentId = "test-agent";
            LinkedList<AgentCommand> list = new LinkedList<>();
            list.add(mockAgentCommand);
            agentCommandManager.agentCommandCache.put(agentId, list);

            AgentCommand result = agentCommandManager.getCommand(agentId);

            assertThat(result).isEqualTo(mockAgentCommand);
            assertThat(agentCommandManager.agentCommandCache.asMap()).isEmpty();
        }

        @Test
        public void commandOfDifferentAgentPresent() throws ExecutionException {
            AgentCommand mockAgentCommand = mock(AgentCommand.class);
            String agentId = "not-the-test-agent";
            LinkedList<AgentCommand> list = new LinkedList<>();
            list.add(mockAgentCommand);
            agentCommandManager.agentCommandCache.put(agentId, list);

            AgentCommand result = agentCommandManager.getCommand("test-agent");

            assertThat(result).isEqualTo(AgentCommand.getEmptyCommand());
            assertThat(agentCommandManager.agentCommandCache.asMap()).containsOnlyKeys(agentId);
        }
    }



}
