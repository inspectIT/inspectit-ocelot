package inspectit.ocelot.configdocsgenerator.model;

import lombok.Value;

import java.util.Set;

/**
 * Model, to store documentable objects of a specific yaml file.
 * Documentable objects can be actions, scopes, rules & metrics.
 *
 */
@Value
public class AgentDocumentation {

    /**
     * file, which contains the documentable objects
     */
    private String filePath;

    /**
     * documentable objects of the file
     */
    private Set<String> objects;
}
