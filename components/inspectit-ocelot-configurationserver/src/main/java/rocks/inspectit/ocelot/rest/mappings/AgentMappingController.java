package rocks.inspectit.ocelot.rest.mappings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.security.audit.AuditDetail;
import rocks.inspectit.ocelot.security.audit.EntityAuditLogger;

import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Controller for managing the agent mappings.
 */
@RestController
@Slf4j
public class AgentMappingController extends AbstractBaseController {

    @Autowired
    private AgentMappingManager mappingManager;

    @Autowired
    private EntityAuditLogger auditLogger;

    /**
     * Returns all existing agent mappings.
     *
     * @return List of {@link AgentMapping}s.
     */
    @GetMapping(value = "mappings")
    public List<AgentMapping> getMappings() {
        return mappingManager.getAgentMappings();
    }

    /**
     * Sets the given list as the {@link AgentMapping}s. The list will replace the existing one. Nothing happens if the
     * new mappings cannot be persisted into a file.
     *
     * @param agentMappings The new {@link AgentMapping}s
     * @throws IOException In case the new mappings cannot be written into a file.
     */
    @PutMapping(value = "mappings")
    public void putMappings(@Valid @RequestBody List<AgentMapping> agentMappings) throws IOException {
        mappingManager.setAgentMappings(agentMappings);
        auditLogger.logEntityCreation(() -> {
            String mappings = agentMappings.stream().map(AgentMapping::getName).collect(Collectors.joining(","));
            return new AuditDetail("Agent Mappings", mappings);
        });
    }

    /**
     * Returns the {@link AgentMapping} with the given name or a 404 response in case it does not exist.
     *
     * @param mappingName the name of the {@link AgentMapping}
     * @return The {@link AgentMapping} with the given name or a 404 if it does not exist
     */
    @GetMapping(value = "mappings/{mappingName}")
    public ResponseEntity<AgentMapping> getMappingByName(@PathVariable("mappingName") String mappingName) {
        Optional<AgentMapping> agentMapping = mappingManager.getAgentMapping(mappingName);
        return ResponseEntity.of(agentMapping);
    }

    /**
     * Deletes the {@link AgentMapping} with the given name.
     *
     * @param mappingName the name of the {@link AgentMapping} to delete
     * @return 200 if the mapping has been deleted or 404 if it does not exist
     * @throws IOException In case of an error during deletion
     */
    @DeleteMapping(value = "mappings/{mappingName}")
    public ResponseEntity deleteMappingByName(@PathVariable("mappingName") String mappingName) throws IOException {
        boolean isDeleted = mappingManager.deleteAgentMapping(mappingName);

        if (isDeleted) {
            auditLogger.logEntityDeletion(() -> new AuditDetail("Agent Mapping", "Name:" + mappingName));
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Adds a new {@link AgentMapping} using the given name. The name contained in the passed {@link AgentMapping} object will
     * be overridden by the specified name parameter. If a {@link AgentMapping} with the given name already existing it will
     * be replaced by the new one.
     * <p>
     * The 'before' and 'after' property can be used to add a mapping at a specific position. It can also be used to move
     * existing mappings. Only one of these properties can be used at a time!
     *
     * @param mappingName  the name to use for the mapping
     * @param agentMapping the agent mapping to add
     * @param before       the name of the element after the added mapping
     * @param after        the name of the element before the added mapping
     * @return 200 in case the operation was successful
     * @throws IOException In case of an error
     */
    @PutMapping(value = "mappings/{mappingName}")
    public ResponseEntity putMapping(@PathVariable("mappingName") String mappingName, @Valid @RequestBody AgentMapping agentMapping, @RequestParam(required = false) String before, @RequestParam(required = false) String after) throws IOException {
        checkArgument(!StringUtils.isEmpty(mappingName), "The mapping name should not be empty or null.");
        checkArgument(before == null || after == null, "The 'before' and 'after' parameters cannot be used together.");

        if (!mappingName.equals(agentMapping.getName())) {
            agentMapping = agentMapping.toBuilder().name(mappingName).build();
        }

        if (before != null) {
            mappingManager.addAgentMappingBefore(agentMapping, before);
        } else if (after != null) {
            mappingManager.addAgentMappingAfter(agentMapping, after);
        } else {
            mappingManager.addAgentMapping(agentMapping);
        }

        auditLogger.logEntityCreation(agentMapping);
        return ResponseEntity.ok().build();
    }
}
