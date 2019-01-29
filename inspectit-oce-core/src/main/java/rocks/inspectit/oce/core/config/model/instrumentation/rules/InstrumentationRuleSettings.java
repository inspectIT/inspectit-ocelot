package rocks.inspectit.oce.core.config.model.instrumentation.rules;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.Map;

@Data
@NoArgsConstructor
public class InstrumentationRuleSettings {

    private boolean enabled;

    @Valid
    private Map<String, Boolean> scopes;

}
