package rocks.inspectit.ocelot.rest.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommand.*;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.Collections;
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
    private AgentCommandDelegator agentCommandDelegator;

    @Autowired
    private AgentCallbackManager agentCallbackManager;

    private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

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
     * @return Returns either a ResponseEntity with the next command as payload or an emtpy payload.
     */
    @PostMapping(value = "agent/command", produces = "application/json")
    public ResponseEntity<String> fetchNewCommand(@RequestHeader Map<String, String> headers, @RequestBody AgentResponse response) throws JsonProcessingException, ExecutionException {
        String agentID = headers.get("x-ocelot-agent-id");
        UUID commandID = null;
        if (response != null) {
            commandID = response.getCommandId();
        }

        if (ObjectUtils.allNotNull(agentID, commandID)) {
            agentCallbackManager.runNextCommandWithId(agentID, commandID);
        }

        AgentCommand agentCommand = agentCommandManager.getCommand(agentID);
        String agentCommandJson = objectWriter.writeValueAsString(AgentCommand.getEmptyCommand());
        if (agentCommand != null) {
            agentCommandJson = objectWriter.writeValueAsString(agentCommand);
        }

        return ResponseEntity.ok().eTag(String.valueOf(agentCommandJson.hashCode())).body(agentCommandJson);
    }

    /**
     * Creates a health-check command for an agent with the given id.
     *
     * @param headers the standard request headers. Must at least contain the key x-ocelot-agent-id.
     *
     * @return Returns either "Agent alive" if the agent with the given id is reachable or "Agent not reachable" if it
     * is not.
     */
    @PostMapping(value = "agent/health", produces = "text/plain")
    public ResponseEntity<?> getHealth(@RequestHeader Map<String, String> headers) throws ExecutionException {
        String agentID = headers.get("x-ocelot-agent-id");
        UUID uuid = UUID.randomUUID();

        AgentCommand command = new AgentCommand(AgentCommandType.GET_HEALTH, agentID, uuid, Collections.emptyList());
        DeferredResult<?> deferredResult = agentCommandDelegator.runCommand(command);

        return (ResponseEntity<?>) deferredResult.getResult();
    }
}
