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
import rocks.inspectit.ocelot.agentcommunication.AgentCallbackManager;
import rocks.inspectit.ocelot.agentcommunication.AgentCommandDispatcher;
import rocks.inspectit.ocelot.agentcommunication.AgentCommandManager;
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
    private AgentCallbackManager agentCallbackManager;

    @ExceptionHandler
    public void e(Exception e) {
        e.printStackTrace();
    }

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
    public ResponseEntity<Command> fetchCommand(@RequestHeader Map<String, String> headers, @RequestParam(required = false, name = "wait-for-command") boolean waitForCommand, @RequestBody(required = false) CommandResponse response) {
        String agentId = headers.get("x-ocelot-agent-id");
        if (agentId == null) {
            return ResponseEntity.badRequest().build();
        }

        if (response != null) {
            UUID commandID = response.getCommandId();

            if (ObjectUtils.allNotNull(agentId, commandID)) {
                agentCallbackManager.handleCommandResponse(commandID, response);
            }
        }

        Command nextCommand = agentCommandManager.getCommand(agentId, waitForCommand);

        if (nextCommand == null) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok().body(nextCommand);
        }
    }
}
