package rocks.inspectit.ocelot.rest.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.CommandsService;
import rocks.inspectit.ocelot.grpc.*;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Controller providing functionality to use agent commands.
 */
@RestController
@Slf4j
public class AgentCommandController extends AbstractBaseController {

    @Autowired
    private CommandsService commandService;

    /**
     * Creates a {@link PingCommand} for an agent with the given id.
     *
     * @param agentId The id of the agent to be pinged.
     *
     * @return Returns OK if the Agent is reachable and Timeout if it is not.
     */
    @GetMapping(value = "command/ping")
    public DeferredResult<ResponseEntity<?>> ping(@RequestParam(value = "agent-id") String agentId) {
        Command command = Command.newBuilder()
                .setCommandId(UUID.randomUUID().toString())
                .setPing(PingCommand.newBuilder())
                .build();
        return commandService.dispatchCommand(agentId, command);
    }

    @GetMapping(value = "command/logs")
    public DeferredResult<ResponseEntity<?>> logs(@RequestParam(value = "agent-id") String agentId, @RequestParam(value = "log-format", required = false, defaultValue = "%d{ISO8601} %-5p %-6r --- [inspectIT] [%15.15t] %-40.40logger{39} : %m%n%rEx") String logFormat) {
        Command command = Command.newBuilder()
                .setCommandId(UUID.randomUUID().toString())
                .setLogs(LogsCommand.newBuilder().setLogFormat(logFormat))
                .build();
        return commandService.dispatchCommand(agentId, command);
    }

    @GetMapping(value = "command/list/classes")
    public DeferredResult<ResponseEntity<?>> listClasses(@RequestParam(value = "agent-id") String agentId, @RequestParam(value = "query") String query) {
        Command command = Command.newBuilder()
                .setCommandId(UUID.randomUUID().toString())
                .setListClasses(ListClassesCommand.newBuilder().setFilter(query))
                .build();
        return commandService.dispatchCommand(agentId, command);
    }

    @GetMapping(value = "command/environment")
    public DeferredResult<ResponseEntity<?>> environment(@RequestParam(value = "agent-id") String agentId) {
        Command command = Command.newBuilder()
                .setCommandId(UUID.randomUUID().toString())
                .setEnvironment(EnvironmentCommand.newBuilder())
                .build();
        return commandService.dispatchCommand(agentId, command);
    }
}
