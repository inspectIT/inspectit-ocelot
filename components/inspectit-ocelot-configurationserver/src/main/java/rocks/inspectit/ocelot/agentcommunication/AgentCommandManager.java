package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.commons.models.command.Command;

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
    final LoadingCache<String, LinkedList<Command>> agentCommandCache;

    public AgentCommandManager() {
        agentCommandCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build(new CacheLoader<String, LinkedList<Command>>() {
                    @Override
                    public LinkedList<Command> load(String key) throws Exception {
                        return new LinkedList<>();
                    }
                });
    }

    /**
     * Takes a String resembling the id of an agent and an instance of {@link Command} and adds the command with
     * the given id as key.
     *
     * @param agentId The id of the agent the command is meant for.
     * @param command An instance of the command to be executed.
     */
    public void addCommand(String agentId, Command command) throws ExecutionException {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent id may never be null!");
        }

        if (command != null) {
            LinkedList<Command> commandList = agentCommandCache.get(agentId);
            commandList.push(command);
        }
    }

    /**
     * Takes a String resembling the id of an agent and returns a command. The commands are ordered by the fifo-principle.
     * The command is then deleted from the queue.
     * Returns null if the agentId is null or if there are no commands to return.
     *
     * @param agentId The ID of the agent for which the command should to be returned.
     *
     * @return The {@link Command} object next in queue for the agent with the given id.
     */
    public Command getCommand(String agentId) throws ExecutionException {
        LinkedList<Command> commandQueue = agentCommandCache.get(agentId);
        if (commandQueue.isEmpty()) {
            agentCommandCache.invalidate(agentId);
            return null;
        }

        Command command = commandQueue.pop();
        if (commandQueue.isEmpty()) {
            agentCommandCache.invalidate(agentId);
        }
        return command;
    }
}
