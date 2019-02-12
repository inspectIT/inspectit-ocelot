package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.GenericDataProviderSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.InstrumentationScopeSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
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
    @NotNull
    private InternalSettings internal;

    /**
     * The configuration for all special sensors.
     */
    @Valid
    @NotNull
    private SpecialSensorSettings special;

    /**
     * Defines which packages of the bootstrap should not be instrumented.
     * All classes from the given packages and their subpackages will be ignored.
     */
    @NotNull
    private Map<@NotBlank String, Boolean> ignoredBootstrapPackages;

    /**
     * Defines which packages of all class loaders should not be instrumented.
     * All classes from the given packages and their subpackages will be ignored.
     */
    @NotNull
    private Map<@NotBlank String, Boolean> ignoredPackages;

    /**
     * All defined custom data providers, the key defines their name.
     * The name is case sensitive!
     */
    @NotNull
    private Map<@NotBlank String, @Valid GenericDataProviderSettings> dataProviders = Collections.emptyMap();

    /**
     * The configuration of the defined scopes. The map's key represents an unique id for the related instrumentation scope.
     */
    @NotNull
    private Map<@NotBlank String, @Valid InstrumentationScopeSettings> scopes = Collections.emptyMap();

    /**
     * The configuration of the defined rules. The map's key represents an unique id for the related instrumentation rule.
     */
    @NotNull
    private Map<@NotBlank String, @Valid InstrumentationRuleSettings> rules = Collections.emptyMap();
}
