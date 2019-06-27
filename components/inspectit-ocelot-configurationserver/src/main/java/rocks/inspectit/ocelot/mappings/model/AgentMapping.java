package rocks.inspectit.ocelot.mappings.model;

import lombok.*;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AgentMapping {

    private String name;

    @Singular
    private List<@NotBlank String> sources;

    @Singular
    private Map<@NotBlank String, @NotBlank String> attributes;
}
