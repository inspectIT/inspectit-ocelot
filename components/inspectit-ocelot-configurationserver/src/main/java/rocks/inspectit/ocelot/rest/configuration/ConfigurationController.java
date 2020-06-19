package rocks.inspectit.ocelot.rest.configuration;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.Map;

@RestController
public class ConfigurationController extends AbstractBaseController {

    @Autowired
    private AgentConfigurationManager configManager;

    /**
     * Returns the {@link InspectitConfig} for the agent with the given name without logging the access in the agent
     * status.
     * Uses text/plain as mime type to ensure that the configuration is presented nicely when opened in a browser
     *
     * @param attributes the attributes of the agents used to select the mapping
     * @return The configuration mapped on the given agent name
     */
    @ApiOperation(value = "Fetch the Agent Configuration without logging the access.", notes = "Reads the configuration for the given agent and returns it as a yaml string." +
            "Does not log the access in the agent status.")
    @GetMapping(value = "configuration/agent-config", produces = "text/plain")
    public ResponseEntity<String> fetchConfiguration(@ApiParam("The agent attributes used to select the correct mapping") @RequestParam Map<String, String> attributes) {
        AgentConfiguration configuration = configManager.getConfiguration(attributes);
        if (configuration == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.ok()
                    .body(configuration.getConfigYaml());
        }
    }
}
