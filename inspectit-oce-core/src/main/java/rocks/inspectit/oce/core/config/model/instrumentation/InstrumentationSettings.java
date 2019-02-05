package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.InstrumentationScopeSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

/**
 * Configuration object for all settings regarding the instrumentation.
 */
@Data
@NoArgsConstructor
public class InstrumentationSettings {

    /**
     * The configuration of internal parameters regarding the instrumentation process.
     */
    @Valid
    private InternalSettings internal;

    /**
     * The configuration for all special sensors.
     */
    @Valid
    private SpecialSensorSettings special;

    /**
     * Defines which packages of the bootstrap should not be instrumented.
     * All classes from the given packages and their subpackages will be ignored.
     */
    private Map<String, Boolean> ignoredBootstrapPackages;

    /**
     * The configuration of the defined scopes. The map's key represents an unique id for the related instrumentation scope.
     */
    @Valid
    @NotNull
    private Map<String, InstrumentationScopeSettings> scopes = Collections.emptyMap();

    /**
     * The configuration of the defined rules. The map's key represents an unique id for the related instrumentation rule.
     */
    @Valid
    @NotNull
    private Map<String, InstrumentationRuleSettings> rules = Collections.emptyMap();
}
