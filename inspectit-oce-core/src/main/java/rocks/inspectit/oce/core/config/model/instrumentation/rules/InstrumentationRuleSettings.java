package rocks.inspectit.oce.core.config.model.instrumentation.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.Map;

/**
 * Data container for the configuration of a instrumentation rule. {@link rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule}
 * instances will be created based on instances of this class.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class InstrumentationRuleSettings {

    /**
     * Defines whether the rule is enabled.
     */
    private boolean enabled;

    /**
     * Defines which scope is used by this rule and whether it is enabled or not. The map's key represents the id of a scope.
     * The value specifies whether it is enabled or not.
     */
    @Valid
    private Map<String, Boolean> scopes;

}
