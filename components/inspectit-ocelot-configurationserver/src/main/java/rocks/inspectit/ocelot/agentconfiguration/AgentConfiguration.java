package rocks.inspectit.ocelot.agentconfiguration;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.Value;
import org.springframework.util.DigestUtils;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An {@link AgentMapping} which has its configuration loaded in-memory.
 * In addition, a cryptographic hash is computed to detect changes of configurations.
 */
@Data
public class AgentConfiguration {

    /**
     * The agent mapping for which this instance represents the loaded configuration.
     */
    private final AgentMapping mapping;

    /**
     * The set of defined documentable objects in this configuration for each file. <br>
     * The map might be initialized after constructing the AgentConfiguration. <br>
     * - Key: the file path <br>
     * - Value: the set of objects, like actions, scopes, rules & metrics
     */
    private Map<String, Set<String>> docsObjectsByFile;

    /**
     * The merged YAML configuration for the given mapping.
     */
    private final String configYaml;

    /**
     * Cryptographic hash for {@link #configYaml}.
     */
    private final String hash;

    @Builder
    private AgentConfiguration(AgentMapping mapping, Map<String, Set<String>> docsObjectsByFile, String configYaml) {
        this.mapping = mapping;
        this.docsObjectsByFile = docsObjectsByFile;
        this.configYaml = configYaml;
        hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
    }
}
