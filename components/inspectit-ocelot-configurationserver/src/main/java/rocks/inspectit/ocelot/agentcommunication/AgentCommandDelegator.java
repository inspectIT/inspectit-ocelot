package rocks.inspectit.ocelot.agentcommunication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.commons.models.AgentCommand;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Has a List with instances of all classes which implement {@link CommandHandler} and implements methods to
 * delegate instances of {@link AgentCommand} to their respective handler.
 */
@Component
public class AgentCommandDelegator {

    @Autowired
    private List<CommandHandler> handlers;

    /**
     * Takes an instance of {@link AgentCommand} and delegates it to the first handler present in the handlers-list
     * with the fitting command type. Returns the handlers return value or null if no fitting handler was found.
     *
     * @param agentCommand The command to be executed.
     *
     * @return The handlers return value or null if no fitting handler was found.
     */
    public DeferredResult<?> runCommand(AgentCommand agentCommand) throws ExecutionException {
        for (CommandHandler handler : handlers) {
            DeferredResult<?> result = handler.handleCommand(agentCommand);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

}
