package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages the callbacks for asynchronous requests between the agent and the frontend.
 * Each callback is represented by an instance of {@link DeferredResult} and can be mapped by a UUID of the respective command.
 */
@Component
public class AgentCallbackManager {

    @Autowired
    @VisibleForTesting
    List<CommandHandler> handlers;

    @VisibleForTesting
    Cache<UUID, DeferredResult<ResponseEntity<?>>> resultCache;

    public AgentCallbackManager() {
        resultCache = CacheBuilder.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Takes an instance of {@link java.util.UUID} and an instance of {@link org.springframework.web.context.request.async.DeferredResult}
     * and adds it to the internal result cache. Throws an exception when the given commandID is null. Does nothing if
     * the given commandResponse is null.
     *
     * @param commandID       An instance of {@link java.util.UUID} which represents the UUID of a existing command.
     * @param commandResponse The instance of {@link org.springframework.web.context.request.async.DeferredResult} to which
     *                        the result of the command should be written.
     *
     * @throws IllegalArgumentException when the given command id is null.
     */
    public void addCommandCallback(UUID commandID, DeferredResult<ResponseEntity<?>> commandResponse) {
        if (commandID == null) {
            throw new IllegalArgumentException("The given command id may never be null!");
        }
        if (commandResponse != null) {
            resultCache.put(commandID, commandResponse);
        }
    }

    /**
     * Takes an instance of {@link java.util.UUID} as well as an instance of {@link CommandResponse}. Delegates
     * the {@link DeferredResult} saved for the given commandID and the given response to the responsible implementation
     * of {@link CommandHandler}.
     *
     * @param commandID The UUID of the command the given response is linked to.
     * @param response  The response which should be handled.
     */
    public void handleCommandResponse(UUID commandID, CommandResponse response) {
        if (commandID == null) {
            throw new IllegalArgumentException("The given command id may never be null!");
        }

        DeferredResult<ResponseEntity<?>> result = resultCache.getIfPresent(commandID);

        if (result != null) {
            resultCache.invalidate(commandID);

            for (CommandHandler handler : handlers) {
                if (handler.canHandle(response)) {
                    handler.handleResponse(response, result);
                }
            }
        }

    }
}
