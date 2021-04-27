package rocks.inspectit.ocelot.agentcommand;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages commands for agent. It provides functionality to add commands for specific agents or retrieve
 * commands for specific agents. Once a command is retrieved it is deleted.
 */
@Service
public class AgentCommandManager {

    @VisibleForTesting
    final Map<String, LinkedList<AgentCommand>> agentCommandMap;

    public AgentCommandManager() {
        agentCommandMap = new ConcurrentHashMap<>();
    }

    public void addCommand(String agentID, AgentCommand command) {
        if(agentID !=null && command != null) {
            if (!this.agentCommandMap.containsKey(agentID)) {
                this.agentCommandMap.put(agentID, new LinkedList<>());
            }
            this.agentCommandMap.get(agentID).add(command);
        }
    }

    /**
     * Takes a String resembling the id of an agent and returns a command. The commands are ordered by the fifo-principle.
     * The command is then deleted from the queue.
     * Returns null if the agentID is null or if there are no commands to return.
     * @param agentID A String resembling the id of an agent.
     * @return The {@link AgentCommand} object next in queue for the agent with the given id.
     */
    public AgentCommand getCommand(String agentID) {
        if(!agentCommandMap.containsKey(agentID)) {
            return null;
        }
        LinkedList<AgentCommand> commandQueue = agentCommandMap.get(agentID);
        if(commandQueue.isEmpty()) {
            return null;
        }

        AgentCommand command = commandQueue.pop();
        if(commandQueue.isEmpty()) {
            agentCommandMap.remove(agentID);
        }

        return command;
    }
}
