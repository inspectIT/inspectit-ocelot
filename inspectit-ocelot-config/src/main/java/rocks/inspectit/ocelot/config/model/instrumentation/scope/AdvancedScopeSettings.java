package rocks.inspectit.ocelot.config.model.instrumentation.scope;

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
     * Defines whether only method should be targeted by the scope's method matcher which are declared in one of the
     * defined interfaces or superclass.
     */
    private boolean instrumentOnlyInheritedMethods = false;

    /**
     * By default, the instrumentation configuration has some restrictions to prevent any negative impact on the target
     * system (e.g. instrumenting all classes). This safety mechanisms can be disabled for the current scope using this flag.
     */
    private boolean disableSafetyMechanisms = false;

}
