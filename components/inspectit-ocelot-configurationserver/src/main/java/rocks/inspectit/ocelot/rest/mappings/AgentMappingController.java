package rocks.inspectit.ocelot.rest.mappings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
public class AgentMappingController extends AbstractBaseController {

    @Autowired
    private AgentMappingManager mappingManager;

    @GetMapping(value = "mappings")
    public List<AgentMapping> getMappings(@RequestParam(defaultValue = "json") String format) {
        return mappingManager.getAgentMappings();
    }

    @PutMapping(value = "mappings")
    public void putMappings(@Valid @RequestBody List<AgentMapping> agentMappings) throws IOException {
        mappingManager.setAgentMappings(agentMappings);
    }

    @GetMapping(value = "mappings/{mappingName}")
    public ResponseEntity<AgentMapping> getMappingByName(@PathVariable("mappingName") String mappingName, @RequestParam(defaultValue = "json") String format) {
        Optional<AgentMapping> agentMapping = mappingManager.getAgentMapping(mappingName);
        return ResponseEntity.of(agentMapping);
    }

    @DeleteMapping(value = "mappings/{mappingName}")
    public ResponseEntity deleteMappingByName(@PathVariable("mappingName") String mappingName, @RequestParam(defaultValue = "json") String format) throws IOException {
        boolean isDeleted = mappingManager.deleteAgentMapping(mappingName);

        if (isDeleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "mappings/{mappingName}")
    public ResponseEntity putMapping(@PathVariable("mappingName") String mappingName, @Valid @RequestBody AgentMapping agentMapping, @RequestParam(required=false) String before, @RequestParam(required=false) String after) throws IOException {
        if (before != null && after != null) {
            throw new IllegalArgumentException("The 'before' and 'after' parameters cannot be used together.");
        }

        agentMapping.setName(mappingName);

        if (before != null) {
            mappingManager.addAgentMappingBefore(agentMapping, before);
        } else if (after != null) {
            mappingManager.addAgentMappingAfter(agentMapping, after);
        } else {
            mappingManager.addAgentMapping(agentMapping);
        }

        return ResponseEntity.ok().build();
    }
}
