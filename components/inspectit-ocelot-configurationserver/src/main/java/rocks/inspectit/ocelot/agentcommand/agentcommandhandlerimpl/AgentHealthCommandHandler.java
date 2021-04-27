package rocks.inspectit.ocelot.agentcommand.agentcommandhandlerimpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommand.*;

import java.util.concurrent.ExecutionException;

/**
 * Handler for the Agent Health check command.
 */
@Slf4j
@Component
public class AgentHealthCommandHandler implements AgentCommandHandler {

    /**
     * The type of command this handler is responsible for.
     */
    private final AgentCommandType commandType = AgentCommandType.GET_HEALTH;

    @Autowired
    private AgentCallbackManager agentCallbackManager;

    @Autowired
    private AgentCommandManager agentCommandManager;

    /**
     * Checks if the given command is of the type GET_HEALTH.
     * If this true, a new agent health command is registered for the agent with the id specified
     * in the given AgentCommand instance. Waits for the command to be executed and returns the commands
     * return value.
     * If the given command is not of type GET_HEALTH, null is returned.
     *
     * @param agentCommand the AgentCommand to be executed.
     *
     * @return Returns either the return value of executeAgentHealthCommand or null if this handler is not responsible
     * for the given command.
     */
    @Override
    public DeferredResult<?> handleCommand(AgentCommand agentCommand) throws ExecutionException {
        if (agentCommand.getCommandType() == commandType) {
            return executeAgentHealthCommand(agentCommand);
        }
        return null;
    }

    /**
     * Creates and executes an agent health-check command. If 30000ms are exceeded without the agent having executed
     * the created command, the agent is assumed to be unreachable. If the agent responses in the given timeframe it is
     * considers as being alive.
     *
     * @param agentCommand the AgentCommand to be executed.
     *
     * @return Returns either a DeferredResult containing a ResponseEntity with the body "Agent alive" or "Agent not reachable".
     */
    private DeferredResult<?> executeAgentHealthCommand(AgentCommand agentCommand) throws ExecutionException {
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(500L);
        Thread currentThread = Thread.currentThread();

        deferredResult.onTimeout(() -> deferredResult.setResult(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body("Request timeout.")));
        Thread agentCommandThread = new Thread(() -> {
            deferredResult.setResult(ResponseEntity.ok("Agent alive"));
            currentThread.interrupt();
        });

        agentCommandManager.addCommand(agentCommand.getAgentId(), agentCommand);
        agentCallbackManager.addCallbackCommand(agentCommand.getAgentId(), agentCommand.getCommandId(), agentCommandThread);
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            log.debug(String.format("Received interruption for agent with id: %s and command id: %s", agentCommand.getAgentId(), agentCommand
                    .getCommandId()));
        }

        if (!deferredResult.isSetOrExpired()) {
            deferredResult.setResult(ResponseEntity.status(HttpStatus.OK).body("Agent not reachable"));
        }

        return deferredResult;
    }
}
