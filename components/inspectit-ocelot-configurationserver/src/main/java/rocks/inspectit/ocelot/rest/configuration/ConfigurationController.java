package rocks.inspectit.ocelot.rest.configuration;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.Map;

/**
 * Controller for endpoints related to configuration files.
 */
@RestController
public class ConfigurationController extends AbstractBaseController {

    /**
     * Event publisher to trigger events upon incoming reload requests.
     */
    @Autowired
    private FileManager fileManager;

    @Autowired
    private AgentConfigurationManager configManager;

    /**
     * Reloads all configuration files present in the servers working directory.
     */
    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Reloads all configuration files.", notes = "Reloads all configuration files present in the " +
            "servers working directory and adds them to the workspace revision.")
    @GetMapping(value = "configuration/reload", produces = "text/plain")
    public void reloadConfiguration() throws GitAPIException {
        fileManager.commitWorkingDirectory();
    }

    /**
     * Returns the {@link InspectitConfig} for the agent with the given name without logging the access in the agent
     * status.
     * Uses text/plain as mime type to ensure that the configuration is presented nicely when opened in a browser
     *
     * @param attributes the attributes of the agents used to select the mapping
     *
     * @return The configuration mapped on the given agent name
     */
    @ApiOperation(value = "Fetch the Agent Configuration without logging the access.", notes = "Reads the configuration for the given agent and returns it as a yaml string." +
            "Does not log the access in the agent status.")
    @GetMapping(value = "configuration/agent-configuration", produces = "text/plain")
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
