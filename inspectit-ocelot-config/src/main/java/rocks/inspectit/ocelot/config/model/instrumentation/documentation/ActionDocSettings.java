package rocks.inspectit.ocelot.config.model.instrumentation.documentation;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

@Data
public class ActionDocSettings extends BaseDocSettings {

    private Map<@NotBlank String, @NotBlank String> _inputDesc = new HashMap<>();
    private String _returnDesc;

}
