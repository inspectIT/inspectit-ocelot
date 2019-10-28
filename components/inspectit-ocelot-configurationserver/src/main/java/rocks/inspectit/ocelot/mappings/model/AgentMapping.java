package rocks.inspectit.ocelot.mappings.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import rocks.inspectit.ocelot.security.audit.AuditDetail;
import rocks.inspectit.ocelot.security.audit.Auditable;

import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The model of the agent mappings.
 */
@Value
@Builder(toBuilder = true)
public class AgentMapping implements Auditable {

    /**
     * The name of this mapping.
     */
    private String name;

    /**
     * The sources which are applied to the matching agents.
     */
    @Singular
    private List<@NotBlank String> sources;

    /**
     * A set of key-value pairs which are used to specify which agent will get the specified sources.
     */
    @Singular
    private Map<@NotBlank String, @NotBlank String> attributes;

    @JsonCreator
    public AgentMapping(@JsonProperty("name") String name, @JsonProperty("sources") List<@NotBlank String> sources, @JsonProperty("attributes") Map<@NotBlank String, @NotBlank String> attributes) {
        this.name = name;
        this.sources = Collections.unmodifiableList(sources);
        this.attributes = Collections.unmodifiableMap(attributes);
    }


    /**
     * Checks if an Agent with a given map of attributes and their values fulfills the requirements of this mapping.
     *
     * @param agentAttributes the attributes to check
     * @return true, if this mapping matches
     */
    public boolean matchesAttributes(Map<String, String> agentAttributes) {
        for (Map.Entry<String, String> pair : attributes.entrySet()) {
            String value = agentAttributes.getOrDefault(pair.getKey(), "");
            if (!value.matches(pair.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    @JsonIgnore
    public AuditDetail getAuditDetail() {
        String identifier = "Name:" + name;
        return new AuditDetail("Agent Mapping", identifier);
    }
}
