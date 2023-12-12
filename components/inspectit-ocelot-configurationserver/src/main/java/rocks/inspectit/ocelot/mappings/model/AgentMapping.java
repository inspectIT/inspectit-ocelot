package rocks.inspectit.ocelot.mappings.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Singular;
import rocks.inspectit.ocelot.file.versioning.Branch;
import rocks.inspectit.ocelot.security.audit.AuditDetail;
import rocks.inspectit.ocelot.security.audit.Auditable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The model of the agent mappings.
 *
 * @param name         The name of this mapping.
 * @param sourceBranch The branch which is used by this mapping.
 * @param sources      The sources which are applied to the matching agents.
 * @param attributes   A set of key-value pairs which are used to specify which agent will get the specified sources.
 */
@Builder(toBuilder = true)
public record AgentMapping(String name, Branch sourceBranch, @Singular List<@NotBlank String> sources,
                           @Singular Map<@NotBlank String, @NotBlank String> attributes) implements Auditable {

    @JsonCreator
    public AgentMapping(
            @JsonProperty("name") String name,
            @JsonProperty("sourceBranch") Branch sourceBranch,
            @JsonProperty("sources") List<@NotBlank String> sources,
            @JsonProperty("attributes") Map<@NotBlank String, @NotBlank String> attributes) {
        this.name = name;
        this.sources = Collections.unmodifiableList(sources);
        this.attributes = Collections.unmodifiableMap(attributes);
        this.sourceBranch = Optional.ofNullable(sourceBranch).orElse(Branch.WORKSPACE);
    }

    /**
     * Checks if an Agent with a given map of attributes and their values fulfills the requirements of this mapping.
     *
     * @param agentAttributes the attributes to check
     * @return true, if this mapping matches
     */
    public boolean matchesAttributes(Map<String, String> agentAttributes) {
        for (Map.Entry<String, String> pair : attributes.entrySet()) {
            String pattern = pair.getValue();
            String value = agentAttributes.getOrDefault(pair.getKey(), "");

            boolean matches = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                    .matcher(value)
                    .matches();
            if (!matches) {
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
