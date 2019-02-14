package rocks.inspectit.oce.core.config.model.instrumentation.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.DataProviderCallSettings;
import rocks.inspectit.oce.core.config.model.validation.ViolationBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
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
    @NotNull
    private Map<@NotBlank String, Boolean> scopes = Collections.emptyMap();

    /**
     * Defines which data is collected at the entryData of the methods instrumented with this rule.
     * The key defines the name of the data which is collected, the value defines how it is collected.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid DataProviderCallSettings> entryData = Collections.emptyMap();

    /**
     * Defines which data is collected at the exitData of the methods instrumented with this rule.
     * The key defines the name of the data which is collected, the value defines how it is collected.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid DataProviderCallSettings> exitData = Collections.emptyMap();

    /**
     * Validates this rule, invoked by {@link InstrumentationSettings#performValidation(ViolationBuilder)}
     *
     * @param container the settings containing this rule
     * @param vios      the violation builder
     */
    public void performValidation(InstrumentationSettings container, ViolationBuilder vios) {
        checkScopesExist(container, vios);
        entryData.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("entryData").atProperty(data)));
        exitData.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("exitData").atProperty(data)));
    }

    private void checkScopesExist(InstrumentationSettings container, ViolationBuilder vios) {
        scopes.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .filter(name -> !container.getScopes().containsKey(name))
                .forEach(name -> {
                    vios.message("Scope '{scope}' does not exist!")
                            .atProperty("scopes")
                            .parameter("scope", name)
                            .buildAndPublish();
                });
    }

}
