package rocks.inspectit.ocelot.rest.agent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.Map;

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
    @Operation(summary = "Fetch the Agent Configuration", description = "Reads the configuration for the given agent and returns it as a yaml string")
    @GetMapping(value = "agent/configuration", produces = "application/x-yaml")
    public ResponseEntity<String> fetchConfiguration(@Parameter(description = "The agent attributes used to select the correct mapping") @RequestParam Map<String, String> attributes, @RequestHeader Map<String, String> headers) {
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
     * @return Returns always an empty payload.
     */
    @PostMapping(value = "agent/command", produces = "application/json")
    public ResponseEntity<String> fetchCommand() {
        // This method is called from the 2.x agents in order to answer a command and/or to request the next
        // Starting from 3.x we do no longer support that method. We still provide this HTTP-interface to prevent a lot
        // of errors during the transition phase.
        return ResponseEntity.noContent().build();
    }
}
