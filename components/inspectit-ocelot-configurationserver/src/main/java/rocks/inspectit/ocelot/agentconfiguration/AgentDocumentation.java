package rocks.inspectit.ocelot.agentconfiguration;

import java.util.Set;

/**
 * Model, to store documentable objects of a specific yaml file.
 * Documentable objects can be actions, scopes, rules & metrics.
 *
 * @param filePath file, which contains the documentable objects
 * @param objects documentable objects of the file
 */
public record AgentDocumentation(String filePath, Set<String> objects) {
}
