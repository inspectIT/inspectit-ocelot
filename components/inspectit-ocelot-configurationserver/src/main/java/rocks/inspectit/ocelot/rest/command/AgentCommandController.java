package rocks.inspectit.ocelot.rest.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.AgentCommandDispatcher;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.concurrent.ExecutionException;

/**
 * Controller providing functionality to use agent commands.
 */
@RestController
@Slf4j
public class AgentCommandController extends AbstractBaseController {

    @Autowired
    private AgentCommandDispatcher commandDispatcher;

    /**
     * Creates a {@link PingCommand} for an agent with the given id.
     *
     * @param agentId The id of the agent to be pinged.
     *
     * @return Returns OK if the Agent is reachable and Timeout if it is not.
     */
    @GetMapping(value = "command/ping/**")
    public DeferredResult<ResponseEntity<?>> ping(@RequestParam(value = "agent-id") String agentId) throws ExecutionException {
        PingCommand command = new PingCommand();
        return commandDispatcher.dispatchCommand(agentId, command);
    }

}
