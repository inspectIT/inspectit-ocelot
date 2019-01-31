package rocks.inspectit.oce.core.config.model.instrumentation.rules.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data container for settings which are used as basis for {@link rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope}.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class InstrumentationScopeSettings {

    /**
     * Defines which classes are targeted by this scope.
     */
    private TypeScope typeScope;

    /**
     * Defines which methods are targeted by this scope.
     */
    private List<MethodMatcherSettings> methodScope;

    /**
     * The scope's advanced settings.
     */
    private AdvancedScopeSettings advanced;

}

