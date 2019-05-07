package rocks.inspectit.ocelot.core.config.model.instrumentation.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.core.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.DataProviderCallSettings;
import rocks.inspectit.ocelot.core.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.core.config.model.validation.ViolationBuilder;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Data container for the configuration of a instrumentation rule. {@link InstrumentationRule}
 * instances will be created based on instances of this class.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class InstrumentationRuleSettings {

    /**
     * Defines whether the rule is enabled.
     */
    private boolean enabled = true;

    /**
     * Defines which scope is used by this rule and whether it is enabled or not. The map's key represents the id of a scope.
     * The value specifies whether it is enabled or not.
     */
    @NotNull
    private Map<@NotBlank String, Boolean> scopes = Collections.emptyMap();

    /**
     * Defines which data is collected at the entry of the methods instrumented with this rule.
     * The key defines the name of the data which is collected, the value defines how it is collected.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid DataProviderCallSettings> entry = Collections.emptyMap();

    /**
     * Defines which data is collected at the exit of the methods instrumented with this rule.
     * The key defines the name of the data which is collected, the value defines how it is collected.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid DataProviderCallSettings> exit = Collections.emptyMap();

    /**
     * Defines which measurements should be taken at the instrumented method.
     * This map maps the name of the metric (=the name of the open census measure) to a data key or a constant value.
     * <p>
     * If the specified value can be parsed using @{@link Double#parseDouble(String)}, the given value is used as constant
     * measurement value for every time a method matching this rule is executed.
     * <p>
     * If the provided value is not parseable as a double, it is assumed that is a data key.
     * In this case the value in the context for the data key is used as value for the given measure.
     * For this reason the value present in the inspectit context for the given data key has to be an instance of {@link Number}.
     * <p>
     * The value in this map can also be null or an empty string, in this case simply no measurement is recorded.
     * In addition the data-key can be null. This can be used to disable the recording of a metric for a rule.
     */
    @NotNull
    private Map<@NotBlank String, String> metrics = Collections.emptyMap();

    /**
     * Stores all configuration options related to tracing.
     */
    @NotNull
    @Valid
    private RuleTracingSettings tracing = new RuleTracingSettings();

    /**
     * Validates this rule, invoked by {@link InstrumentationSettings#performValidation(InspectitConfig, ViolationBuilder)}
     *
     * @param container      the root config containing this rule
     * @param definedMetrics all metrics which have been defined in the {@link MetricsSettings}
     * @param vios           the violation builder
     */
    public void performValidation(InstrumentationSettings container, Set<String> definedMetrics, ViolationBuilder vios) {
        checkScopesExist(container, vios);
        checkMetricsDefined(definedMetrics, vios);
        entry.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("entry").atProperty(data)));
        exit.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("exit").atProperty(data)));
    }

    private void checkMetricsDefined(Set<String> definedMetrics, ViolationBuilder vios) {
        metrics.keySet().stream()
                .filter(m -> !definedMetrics.contains(m))
                .forEach(m -> vios.atProperty("metrics")
                        .message("Metric '{metric}' is not defined in inspectit.metrics.definitions!")
                        .parameter("metric", m)
                        .buildAndPublish());
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
