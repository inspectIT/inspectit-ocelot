package rocks.inspectit.ocelot.agentconfiguration;

import lombok.Value;
import org.springframework.util.DigestUtils;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.nio.charset.Charset;

/**
 * An {@link AgentMapping} which has its configuration loaded in-memory.
 * In addition a cryptographic hash is computed to detect changes of configurations.
 */
@Value
public class AgentConfiguration {

    /**
     * The agent mapping for which this instance represents the loaded configuration.
     */
    private AgentMapping mapping;

    /**
     * The merged YAML configuration for the given mapping.
     */
    private String configYaml;

    /**
     * Cryptographic hash for {@link #configYaml}.
     */
    private String hash;

    public AgentConfiguration(AgentMapping mapping, String configYaml) {
        this.mapping = mapping;
        this.configYaml = configYaml;
        hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
    }
}
