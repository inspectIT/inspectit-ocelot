package rocks.inspectit.oce.core.config.model.instrumentation.rules.scope;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdvancedScopeSettings {

    private Boolean instrumentOnlyAbstractClasses = false;

    private Boolean instrumentOnlyInheritedMethods = false;

}
