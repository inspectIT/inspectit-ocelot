package rocks.inspectit.ocelot.agentcommunication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.commons.models.command.Command;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Has a List with instances of all classes which implement {@link CommandHandler} and implements methods to
 * delegate instances of {@link Command} to their respective handler.
 */
@Component
public class AgentCommandDispatcher {

    @Autowired
    private List<CommandHandler> handlers;

    @Autowired
    private AgentCallbackManager callbackManager;

    @Autowired
    private AgentCommandManager commandManager;

    /**
     * Takes an instance of {@link Command} and delegates it to the first handler present in the handlers-list
     * with the fitting command type. Returns the handlers return value. Throws an {@link IllegalStateException} when
     * no matching handler is present or a handler returns null.
     *
     * @param agentId The id of the agent for which the command should be executed.
     * @param command The command to be executed.
     * @throws IllegalStateException If no handler for the given command-type is present or a handler returned null.
     *
     * @return The handlers return value.
     */
    public DeferredResult<ResponseEntity<?>> runCommand(String agentId, Command command) throws ExecutionException {
        DeferredResult<ResponseEntity<?>> result = null;

        for (CommandHandler handler : handlers) {
            if (handler.canHandle(command)) {
                result = handler.prepareResponse(agentId, command);
            }
        }

        if (result == null) {
            throw new IllegalStateException(String.format("Handler returned invalid response value null or no handler exists for %s", command
                    .getClass()));
        }

        callbackManager.addCommandCallback(command.getCommandId(), result);
        commandManager.addCommand(agentId, command);

        return result;
    }

}
