package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.config.model.AgentCommandSettings;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class manages commands for agents. It provides functionality to add commands for specific agents or retrieve
 * commands for specific agents. Once a command is retrieved it is deleted.
 */
@Slf4j
@Service
public class AgentCommandManager {

    @Autowired
    private InspectitServerSettings configuration;

    @VisibleForTesting
    LoadingCache<String, BlockingQueue<Command>> agentCommandCache;

    @PostConstruct
    public void postConstruct() {
        AgentCommandSettings commandSettings = configuration.getAgentCommand();
        long commandTimeout = commandSettings.getCommandTimeout().toMillis();
        int commandQueueSize = commandSettings.getCommandQueueSize();

        agentCommandCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(commandTimeout, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<String, BlockingQueue<Command>>() {
                @Override
                public BlockingQueue<Command> load(String key) {
                    return new LinkedBlockingQueue<>(commandQueueSize);
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
            BlockingQueue<Command> commandList = agentCommandCache.get(agentId);
            boolean success = commandList.offer(command);
        }
    }

    /**
     * Takes a String resembling the id of an agent and returns a command. The commands are ordered by the fifo-principle.
     * The command is then deleted from the queue.
     * Returns null if the agentId is null or if there are no commands to return.
     *
     * @param agentId        The ID of the agent for which the command should to be returned.
     * @param waitForCommand Whether it should be waited until a command appears
     * @return The {@link Command} object next in queue for the agent with the given id.
     */
    public Command getCommand(String agentId, boolean waitForCommand) {
        try {
            BlockingQueue<Command> commandQueue = agentCommandCache.get(agentId);
            Command command;
            if (waitForCommand) {
                AgentCommandSettings commandSettings = configuration.getAgentCommand();
                long timeout = commandSettings.getAgentPollingTimeout().toMillis();
                command = commandQueue.poll(timeout, TimeUnit.MILLISECONDS);
            } else {
                command = commandQueue.poll();
            }

            if (command == null) {
                agentCommandCache.invalidate(agentId);
            }
            return command;
        } catch (ExecutionException | InterruptedException e) {
            log.error("Exception while getting an agent command.", e);
            return null;
        }
    }
}
