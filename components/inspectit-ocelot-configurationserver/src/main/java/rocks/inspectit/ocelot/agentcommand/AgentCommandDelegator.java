package rocks.inspectit.ocelot.agentcommand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class AgentCommandDelegator {

    @Autowired
    private List<AgentCommandHandler> handlers;

    /**
     * Takes an instance of {@link AgentCommand} and delegates it to the first handler present in the handlers-list
     * with the fitting command type. Returns the handlers return value or null if no fitting handler was found.
     *
     * @param agentCommand The command to be executed.
     *
     * @return The handlers return value or null if no fitting handler was found.
     */
    public DeferredResult<?> runCommand(AgentCommand agentCommand) throws ExecutionException {
        for (AgentCommandHandler handler : handlers) {
            DeferredResult<?> result = handler.handleCommand(agentCommand);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

}
