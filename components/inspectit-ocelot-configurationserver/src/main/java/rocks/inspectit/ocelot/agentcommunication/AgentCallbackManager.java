package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Manages the callbacks for asynchronous requests between the agent and the frontend.
 * Each callback is represented by an instance of {@link DeferredResult} and can be mapped by a UUID of the respective command.
 */
@Component
public class AgentCallbackManager implements RemovalListener<UUID, DeferredResult<ResponseEntity<?>>> {

    @Autowired
    private InspectitServerSettings configuration;

    @Autowired
    @VisibleForTesting
    List<CommandHandler> handlers;

    @VisibleForTesting
    Cache<UUID, DeferredResult<ResponseEntity<?>>> resultCache;

    @PostConstruct
    public void postConstruct() {
        Duration responseTimeout = configuration.getAgentCommand().getResponseTimeout();
        long responseTimeoutMs = responseTimeout.toMillis();

        resultCache = CacheBuilder.newBuilder()
                .expireAfterWrite(responseTimeoutMs, TimeUnit.MILLISECONDS)
                .removalListener(this) // done so that deferred results which time out throw a timeout exception
                .build();
    }

    @Override
    public void onRemoval(RemovalNotification<UUID, DeferredResult<ResponseEntity<?>>> resultEntity) {
        if (resultEntity.wasEvicted()) {
            DeferredResult<ResponseEntity<?>> result = resultEntity.getValue();
            TimeoutException exception = new TimeoutException(resultEntity.getCause().toString());
            result.setErrorResult(exception);
        }
    }

    /**
     * Takes an instance of {@link UUID} and an instance of {@link DeferredResult}
     * and adds it to the internal result cache. Throws an {@link IllegalArgumentException} when the given commandId is
     * null. Does nothing if the given commandResponse is null.
     *
     * @param commandId       An instance of {@link UUID} which represents the UUID of a existing command.
     * @param commandResponse The instance of {@link DeferredResult} to which the result of the command should be written.
     *
     * @throws IllegalArgumentException when the given command id is null.
     */
    public void addCommandCallback(UUID commandId, DeferredResult<ResponseEntity<?>> commandResponse) {
        if (commandId == null) {
            throw new IllegalArgumentException("The given command id must not be null!");
        }
        if (commandResponse != null) {
            resultCache.put(commandId, commandResponse);
        }
    }

    /**
     * Takes an instance of {@link UUID} as well as an instance of {@link CommandResponse}. Delegates
     * the {@link DeferredResult} saved for the given commandId and the given response to the responsible implementation
     * of {@link CommandHandler}.
     *
     * @param commandId The UUID of the command the given response is linked to.
     * @param response  The response which should be handled.
     */
    public void handleCommandResponse(UUID commandId, CommandResponse response) {
        if (commandId == null) {
            throw new IllegalArgumentException("The given command id must not be null!");
        }

        DeferredResult<ResponseEntity<?>> result = resultCache.getIfPresent(commandId);

        if (result != null) {
            resultCache.invalidate(commandId);

            for (CommandHandler handler : handlers) {
                if (handler.canHandle(response)) {
                    handler.handleResponse(response, result);
                }
            }
        }

    }
}
