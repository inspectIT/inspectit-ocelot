package rocks.inspectit.oce.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Data container for advanced settings of an {@link InstrumentationScopeSettings}.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class AdvancedScopeSettings {

    /**
     * Defines whether only abstract classes should be targeted by the scope's type matcher.
     */
    private Boolean instrumentOnlyAbstractClasses = false;

    /**
     * Defines whether only inherited method should be targeted by the scope's method matcher.
     */
    private Boolean instrumentOnlyInheritedMethods = false;

}
