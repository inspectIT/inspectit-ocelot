package rocks.inspectit.ocelot.agentcommand;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AgentCommandManagerTest {

    @InjectMocks
    AgentCommandManager agentCommandManager;

    @Nested
    class AddCommand {

        @Test
        public void addCommandNewAgent() {
            AgentCommand mockAgentCommand = mock(AgentCommand.class);
            String agentId = "test-agent";

            agentCommandManager.addCommand(agentId, mockAgentCommand);

            Map<String, LinkedList<AgentCommand>> result = agentCommandManager.agentCommandMap;
            assertThat(result).containsOnlyKeys(agentId);
            List<AgentCommand> commandList = result.get(agentId);
            assertThat(commandList).containsExactly(mockAgentCommand);
        }

        @Test
        public void ignoresNullId() {
            AgentCommand mockAgentCommand = mock(AgentCommand.class);

            agentCommandManager.addCommand(null, mockAgentCommand);

            Map<String, LinkedList<AgentCommand>> result = agentCommandManager.agentCommandMap;
            assertThat(result).isEmpty();
        }

        @Test
        public void ignoresNullCommand() {
            String agentId = "test-agent";

            agentCommandManager.addCommand(agentId, null);

            Map<String, LinkedList<AgentCommand>> result = agentCommandManager.agentCommandMap;
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetCommand {

        @Test
        public void noCommandPresent() {
            String agentId = "test-agent";

            AgentCommand result = agentCommandManager.getCommand(agentId);

            assertThat(result).isNull();
        }

        @Test
        public void commandPresent() {
            AgentCommand mockAgentCommand = mock(AgentCommand.class);
            String agentId = "test-agent";
            agentCommandManager.addCommand(agentId, mockAgentCommand);

            AgentCommand result = agentCommandManager.getCommand(agentId);

            assertThat(result).isEqualTo(mockAgentCommand);
            assertThat(agentCommandManager.agentCommandMap).isEmpty();
        }

        @Test
        public void commandOfDifferentAgentPresent() {
            AgentCommand mockAgentCommand = mock(AgentCommand.class);
            String agentId = "not-the-test-agent";
            agentCommandManager.addCommand(agentId, mockAgentCommand);

            AgentCommand result = agentCommandManager.getCommand("test-agent");

            assertThat(result).isNull();
            assertThat(agentCommandManager.agentCommandMap).containsOnlyKeys(agentId);
        }
    }



}
