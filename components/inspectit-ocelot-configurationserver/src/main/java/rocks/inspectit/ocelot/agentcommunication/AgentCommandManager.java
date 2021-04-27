package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.commons.models.AgentCommand;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class manages commands for agents. It provides functionality to add commands for specific agents or retrieve
 * commands for specific agents. Once a command is retrieved it is deleted.
 */
@Service
public class AgentCommandManager {

    @VisibleForTesting
    final LoadingCache<String, LinkedList<AgentCommand>> agentCommandCache;

    public AgentCommandManager() {
        agentCommandCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build(new CacheLoader<String, LinkedList<AgentCommand>>() {
                    @Override
                    public LinkedList<AgentCommand> load(String key) {
                        return new LinkedList<>();
                    }
                });
    }

    /**
     * Takes a String resembling the id of an agent and an instance of {@link AgentCommand} and adds the command with
     * the given id as key.
     *
     * @param agentID The id of the agent the command is meant for.
     * @param command An instance of the command to be executed.
     */
    public void addCommand(String agentID, AgentCommand command) throws ExecutionException {
        if (agentID != null && command != null) {
            LinkedList<AgentCommand> agentCommandList = agentCommandCache.get(agentID);
            agentCommandList.push(command);
        }
    }

    /**
     * Takes a String resembling the id of an agent and returns a command. The commands are ordered by the fifo-principle.
     * The command is then deleted from the queue.
     * Returns an empty AgentCommand if the agentID is null or if there are no commands to return.
     *
     * @param agentID A String resembling the id of an agent.
     *
     * @return The {@link AgentCommand} object next in queue for the agent with the given id.
     */
    public AgentCommand getCommand(String agentID) throws ExecutionException {
        LinkedList<AgentCommand> commandQueue = agentCommandCache.get(agentID);
        if (commandQueue.isEmpty()) {
            agentCommandCache.invalidate(agentID);
            return AgentCommand.getEmptyCommand();
        }

        AgentCommand command = commandQueue.pop();
        if (commandQueue.isEmpty()) {
            agentCommandCache.invalidate(agentID);
        }
        return command;
    }
}
