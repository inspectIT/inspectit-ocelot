package rocks.inspectit.ocelot.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.data.DataSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.ocelot.config.validation.ViolationBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

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
    private Map<@NotBlank String, Boolean> ignoredBootstrapPackages = Collections.emptyMap();

    /**
     * Defines which packages of all class loaders should not be instrumented.
     * All classes from the given packages and their subpackages will be ignored.
     */
    @NotNull
    private Map<@NotBlank String, Boolean> ignoredPackages = Collections.emptyMap();

    /**
     * All defined custom generic actions, the key defines their name.
     * The name is case sensitive!
     */
    @NotNull
    private Map<@NotBlank String, @Valid GenericActionSettings> actions = Collections.emptyMap();


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

    /**
     * Defines the behaviour of the data regarding context propagation.
     * E.g. is data propagated up and/or down, is it visible as a Tag for metrics collection?
     */
    @NotNull
    private Map<@NotBlank String, @Valid DataSettings> data = Collections.emptyMap();

    /**
     * Defines whether lambda classes should be excluded from the declared scope.
     */
    private boolean excludeLambdas = true;

    /**
     * Allows all nested configs to evaluate context sensitive config properties regarding their correctness.
     * This is called by {@link InspectitConfig#performValidation(ViolationBuilder)}
     *
     * @param container the object containing this instance
     * @param vios      the violation output
     */
    public void performValidation(InspectitConfig container, ViolationBuilder vios) {
        Set<String> declaredMetrics = container.getMetrics().getDefinitions().keySet();
        rules.forEach((name, r) ->
                r.performValidation(this, declaredMetrics, vios.atProperty("rules").atProperty(name)));

        HashSet<String> verified = new HashSet<>();
        // Verifies that scopes that are defined as Exclude also exist
        scopes.forEach((name, s) ->
                s.performValidation(name, this, vios.atProperty("scopes").atProperty(name), verified));
    }

}
