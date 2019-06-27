package rocks.inspectit.ocelot.mappings.model;

import lombok.*;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * The model of the agent mappings.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
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
}
