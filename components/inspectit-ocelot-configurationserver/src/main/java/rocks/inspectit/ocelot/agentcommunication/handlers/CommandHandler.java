package rocks.inspectit.ocelot.agentcommunication.handlers;

import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.commons.models.AgentCommand;

import java.util.concurrent.ExecutionException;

public interface CommandHandler {

    /**
     * Takes an instance of {@link AgentCommand} and executes it.
     *
     * @param agentCommand the AgentCommand to be Executed.
     *
     * @return An instance of DeferredResult containing the result of the command.
     */
    DeferredResult<?> handleCommand(AgentCommand agentCommand) throws ExecutionException;

}
