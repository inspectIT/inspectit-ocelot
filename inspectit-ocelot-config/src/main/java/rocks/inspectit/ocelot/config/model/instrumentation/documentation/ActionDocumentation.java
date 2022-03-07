package rocks.inspectit.ocelot.config.model.instrumentation.documentation;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Data container for information specific to actions to be used to generate a configuration documentation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActionDocumentation extends BaseDocumentation {

    /**
     * The keys of the map are the names of one of the action's inputs, the values are a description for the input.
     */
    private Map<@NotBlank String, @NotBlank String> inputs = Collections.emptyMap();

    /**
     * A description of the action's return value.
     */
    private String returnValue = "";

}