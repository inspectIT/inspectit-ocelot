package rocks.inspectit.ocelot.mappings.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The model of the agent mappings.
 */
@Value
@Builder(toBuilder = true)
public class AgentMapping {

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
}
