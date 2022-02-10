package rocks.inspectit.ocelot.config.model.instrumentation.documentation;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper=true)
public class ActionDocSettings extends BaseDocSettings {

    private Map<@NotBlank String, @NotBlank String> inputDesc = new HashMap<>();
    private String returnDesc;

}
