package rocks.inspectit.ocelot.rest.agent;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.*;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * The rest controller providing the interface used by the agent for configuration fetching.
 */
@RestController
@Slf4j
public class AgentController extends AbstractBaseController {

    @Autowired
    private AgentConfigurationManager configManager;

    @Autowired
    private AgentStatusManager statusManager;

    @Autowired
    private AgentCommandManager agentCommandManager;

    @Autowired
    private AgentCommandDispatcher commandDispatcher;

    @Autowired
    private AgentCallbackManager agentCallbackManager;

    /**
     * Returns the {@link InspectitConfig} for the agent with the given name.
     * Uses text/plain as mime type to ensure that the configuration is presented nicely when opened in a browser
     *
     * @param attributes the attributes of the agents used to select the mapping
     *
     * @return The configuration mapped on the given agent name
     */
    @ApiOperation(value = "Fetch the Agent Configuration", notes = "Reads the configuration for the given agent and returns it as a yaml string")
    @GetMapping(value = "agent/configuration", produces = "text/plain")
    public ResponseEntity<String> fetchConfiguration(@ApiParam("The agent attributes used to select the correct mapping") @RequestParam Map<String, String> attributes, @RequestHeader Map<String, String> headers) {
        log.debug("Fetching the agent configuration for agent ({})", attributes.toString());
        AgentConfiguration configuration = configManager.getConfiguration(attributes);
        statusManager.notifyAgentConfigurationFetched(attributes, headers, configuration);
        if (configuration == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.ok().eTag(configuration.getHash()).body(configuration.getConfigYaml());
        }
    }

    /**
     * Returns the command next up in line for the agent with the given id.
     * If no command exists for the given agent, an empty request is returned.
     *
     * @param headers the standard request headers of the agent. Must at least contain the key x-ocelot-agent-id.
     *
     * @return Returns either a ResponseEntity with the next command as payload or an empty payload.
     */
    @PostMapping(value = "agent/command", produces = "application/json")
    public ResponseEntity<Command> fetchNewCommand(@RequestHeader Map<String, String> headers, @RequestBody CommandResponse response) throws ExecutionException {
        String agentID = headers.get("x-ocelot-agent-id");
        UUID commandID = null;
        if (response != null) {
            commandID = response.getCommandId();
        }

        if (ObjectUtils.allNotNull(agentID, commandID)) {
            agentCallbackManager.handleCommandResponse(commandID, response);
        }

        Command command = agentCommandManager.getCommand(agentID);
        return ResponseEntity.ok().body(command);
    }

    /**
     * Creates a {@link PingCommand} for an agent with the given id.
     *
     * @param agentId The id of the agent to be pinged.
     *
     * @return Returns OK if the Agent is reachable and Timeout if it is not.
     */
    @GetMapping(value = "agent/health/**", produces = "text/plain")
    public DeferredResult<ResponseEntity<?>> getHealth(@RequestParam(value = "agent-id") String agentId) throws ExecutionException {
        PingCommand command = new PingCommand();
        return commandDispatcher.runCommand(agentId, command);
    }
}
