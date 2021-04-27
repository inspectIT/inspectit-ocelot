package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Manages the callbacks for asynchronous requests between the agent and the frontend.
 * Each callback is represented by an instance of Thread and can be mapped by a combination of the UUID of the
 * request and the respective agent id.
 */
@Component
public class AgentCallbackManager {

    @VisibleForTesting
    LoadingCache<String, LinkedList<Thread>> agentCallBackCache;

    public AgentCallbackManager() {
        agentCallBackCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build(new CacheLoader<String, LinkedList<Thread>>() {
                    @Override
                    public LinkedList<Thread> load(String key) {
                        return new LinkedList<>();
                    }
                });
    }

    /**
     * Takes a String resembling the id of an agent as well as UUID resembling the id of the command and a Thread. Saves the Thread
     * with the agentID and the UUID instance as keys.
     *
     * @param agentID       The agent this command is meant for.
     * @param commandID     The id of the command.
     * @param commandThread The code to be executed when the corresponding agent sends a request to the server.
     */
    public void addCallbackCommand(String agentID, UUID commandID, Thread commandThread) throws ExecutionException {
        if (ObjectUtils.allNotNull(agentID, commandID, commandThread)) {
            String identifier = agentID + commandID.toString();
            LinkedList<Thread> agentCallBackList = agentCallBackCache.get(identifier);
            agentCallBackList.push(commandThread);
        }
    }

    /**
     * Takes a String resembling the id of an agent as well as UUID resembling the id of the command.
     * Runs the command next in line for this combination of UUID and agent id.
     *
     * @param agentID   The agent this command is meant for.
     * @param commandID The id of the command to be executed.
     */
    public void runNextCommandWithId(String agentID, UUID commandID) throws ExecutionException {
        if (ObjectUtils.allNotNull(agentID, commandID)) {

            String identifier = agentID + commandID.toString();

            LinkedList<Thread> agentCallBackList = agentCallBackCache.get(identifier);
            Thread command = agentCallBackList.pop();
            if (command != null) {
                command.start();
            }
            if (agentCallBackList.isEmpty()) {
                agentCallBackCache.invalidate(identifier);
            }
        }
    }
}
