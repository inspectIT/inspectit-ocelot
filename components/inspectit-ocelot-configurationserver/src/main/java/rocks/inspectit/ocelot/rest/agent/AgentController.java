package rocks.inspectit.ocelot.rest.agent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.AgentCallbackManager;
import rocks.inspectit.ocelot.agentcommunication.AgentCommandManager;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
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

    @Autowired
    private AgentService agentService;

    @ExceptionHandler
    public void e(Exception e) {
        log.warn("Error occurred calling AgentController: ", e);
    }

    /**
     * Returns the {@link InspectitConfig} for the agent with the given name.
     * Uses text/plain as mime type to ensure that the configuration is presented nicely when opened in a browser
     *
     * @param attributes the attributes of the agents used to select the mapping
     *
     * @return The configuration mapped on the given agent name
     */
    @Operation(summary = "Fetch the Agent Configuration", description = "Reads the configuration for the given agent and returns it as a yaml string")
    @GetMapping(value = {"agent/configuration", "agent/configuration/"}, produces = "application/x-yaml")
    public ResponseEntity<String> fetchConfiguration(@Parameter(description = "The agent attributes used to select the correct mapping") @RequestParam Map<String, String> attributes, @RequestHeader Map<String, String> headers) {
        log.debug("Fetching the agent configuration for agent ({})", attributes.toString());
        log.debug("Receiving agent headers ({})", headers.toString());
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
    @PostMapping(value = {"agent/command", "agent/command/"}, produces = "application/json")
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

    /**
     * Returns the data for building the downloadable support archive for the agent with the given name in the frontend.
     *
     * @param attributes the attributes of the agents used to select the appropriate data.
     *
     * @return The data used in the support archive.
     */
    @Operation(summary = "Fetch an Agents Data for Downloading a Support Archive", description = "Bundles useful information for debugging issues raised in support tickets.")
    @GetMapping(value = {"agent/supportArchive", "agent/supportArchive/"}, produces = "application/json")
    public DeferredResult<ResponseEntity<?>> fetchSupportArchive(@Parameter(description = "The agent attributes used to retrieve the correct data") @RequestParam Map<String, String> attributes) throws ExecutionException {
        return agentService.buildSupportArchive(attributes, configManager);
    }
}
