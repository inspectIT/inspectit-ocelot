package rocks.inspectit.ocelot.agentstatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * Status information for an agent identified by his set of attributes.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentStatus {

    /**
     * Meta information about the agent, in case the configuration was fetched by an agent.
     */
    private AgentMetaInformation metaInformation;

    /**
     * The attributes with which the corresponding agent connected
     */
    private Map<String, String> attributes;

    /**
     * The last point in time when the agent fetched the configuration
     */
    private Date lastConfigFetch;

    /**
     * The mapping which was used to serve the agents configuration.
     * If null, this means that no matching mapping was found.
     */
    private String mappingName;

    /**
     * The branch of which the mapping delivered to the agent originates.
     */
    private String sourceBranch;
}
