package rocks.inspectit.ocelot.agentcommand;

import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.ExecutionException;

public interface AgentCommandHandler {

    /**
     * Takes an instance of {@link AgentCommand} and executes it.
     *
     * @param agentCommand the AgentCommand to be Executed.
     *
     * @return An instance of DeferredResult containing the result of the command.
     */
    DeferredResult<?> handleCommand(AgentCommand agentCommand) throws ExecutionException;

}
