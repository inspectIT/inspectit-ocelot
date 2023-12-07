package rocks.inspectit.ocelot.rest.configuration;

import inspectit.ocelot.configdocsgenerator.ConfigDocsGenerator;
import inspectit.ocelot.configdocsgenerator.model.ConfigDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentconfiguration.ObjectStructureMerger;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.rest.file.DefaultConfigController;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for endpoints related to configuration files.
 */
@RestController
@Slf4j
public class ConfigurationController extends AbstractBaseController {

    /**
     * Event publisher to trigger events upon incoming reload requests.
     */
    @Autowired
    private FileManager fileManager;

    @Autowired
    private AgentConfigurationManager configManager;

    @Autowired
    private AgentMappingManager mappingManager;

    @Autowired
    private DefaultConfigController defaultConfigController;

    // Not final to make mocking in test possible
    private ConfigDocsGenerator configDocsGenerator = new ConfigDocsGenerator();

    // Not final to make mocking in test possible
    private Yaml yaml = new Yaml();

    /**
     * Reloads all configuration files present in the servers working directory.
     */
    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @Operation(summary = "Reloads all configuration files.", description = "Reloads all configuration files present in the " + "servers working directory and adds them to the workspace revision.")
    @GetMapping(value = {"configuration/reload", "configuration/reload/"}, produces = "application/x-yaml")
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
    @Operation(summary = "Fetch the Agent Configuration without logging the access.", description = "Reads the configuration for the given agent and returns it as a yaml string." + "Does not log the access in the agent status.")
    @GetMapping(value = {"configuration/agent-configuration", "configuration/agent-configuration/"}, produces = "application/x-yaml")
    public ResponseEntity<String> fetchConfiguration(@Parameter(description = "The agent attributes used to select the correct mapping") @RequestParam Map<String, String> attributes) {
        AgentConfiguration configuration = configManager.getConfiguration(attributes);
        if (configuration == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.ok().body(configuration.getConfigYaml());
        }
    }

    /**
     * Returns the {@link ConfigDocumentation} for the configuration matching the given AgentMapping.
     *
     * @param mappingName Name to identify the AgentMapping.
     *
     * @return ConfigDocumentation for the configuration matching the given AgentMapping.
     */
    @Operation(summary = "Get the full configuration documentation of an AgentMappings config, based on the AgentMapping name.")
    @GetMapping(value = {"configuration/documentation", "configuration/documentation/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getConfigDocumentation(@Parameter(description = "The name of the AgentMapping the configuration documentation should be for.") @RequestParam(name = "agent-mapping") String mappingName, @Parameter(description = "Whether the shown documentation should include the merged in Default Config as well.") @RequestParam(name = "include-default") Boolean includeDefault) {

        ConfigDocumentation configDocumentation = null;

        Optional<AgentMapping> agentMapping = mappingManager.getAgentMapping(mappingName);

        if (agentMapping.isPresent()) {

            AgentConfiguration configuration = configManager.getConfigurationForMapping(agentMapping.get());
            String configYaml = configuration.getConfigYaml();

            try {
                if (includeDefault) {
                    Map<String, String> defaultYamls = defaultConfigController.getDefaultConfigContent();

                    Object combined = yaml.load(configYaml);
                    if (combined == null) {
                        combined = Collections.emptyMap();
                    }
                    for (String defaultYaml : defaultYamls.values()) {
                        Object loadedYaml = yaml.load(defaultYaml);
                        combined = ObjectStructureMerger.merge(combined, loadedYaml);
                    }
                    configYaml = yaml.dump(combined);
                }

                configDocumentation = configDocsGenerator.generateConfigDocs(configYaml);

            } catch (Exception e) {
                log.error("Config Documentation could not be generated due to Exception.", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(String.format("Config Documentation for given AgentMapping '%s' could not be generated due to the following error: %s.", mappingName, e.getMessage()));
            }
        }

        if (configDocumentation != null) {
            return ResponseEntity.ok().body(configDocumentation);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(String.format("No AgentMapping found with the name '%s'.", mappingName));
        }
    }
}
