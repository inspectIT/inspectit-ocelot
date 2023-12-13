package rocks.inspectit.ocelot.agentconfiguration;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.util.List;

/**
 * Data container, which contains a list of names of objects like actions, scopes, rules and metrics
 * that are documented within one file
 */
@Data
@Builder
public class AgentDocumentation {

    /**
     * The file, which contains the objects
     */
    private String filePath;

    /**
     * The list of documented objects
     */
    private List<String> documentedObjects;
}
