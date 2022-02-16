package rocks.inspectit.ocelot.config.model.instrumentation.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.documentation.BaseDocumentation;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.validation.ViolationBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Data container for the configuration of a instrumentation rule. {@link rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule}
 * instances will be created based on instances of this class.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class InstrumentationRuleSettings {

    /**
     * Documentation for Config-Docs generation.
     */
    private BaseDocumentation docs;

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
     * Rules can include other rules.
     * If a rule is included it has the same effect as adding all the scopes of this rule to the included one.
     * <p>
     * This means that all actions, tracing settings and metrics recordings of the included rule are also included.
     * <p>
     * The keys of this map are the names of the rules to include, the value must be "true" for the include to be active.
     * Note that a rule will only be included if it also is enabled!
     */
    @NotNull
    private Map<@NotBlank String, Boolean> include = Collections.emptyMap();

    /**
     * Defines the action to execute before {@link #entry}.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid ActionCallSettings> preEntry = Collections.emptyMap();

    /**
     * Defines which data is collected at the entry of the methods instrumented with this rule.
     * The key defines the name of the data which is collected, the value defines how it is collected.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid ActionCallSettings> entry = Collections.emptyMap();


    /**
     * Defines the action to execute after {@link #entry}.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid ActionCallSettings> postEntry = Collections.emptyMap();

    /**
     * Defines the action to execute before {@link #exit}.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid ActionCallSettings> preExit = Collections.emptyMap();

    /**
     * Defines which data is collected at the exit of the methods instrumented with this rule.
     * The key defines the name of the data which is collected, the value defines how it is collected.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid ActionCallSettings> exit = Collections.emptyMap();

    /**
     * Defines the action to execute after {@link #exit}.
     */
    @NotNull
    private Map<@NotBlank String, @NotNull @Valid ActionCallSettings> postExit = Collections.emptyMap();

    /**
     * Defines which metrics should be recorded at the instrumented method.
     * This map maps the name of the metric (=the name of the open census measure) to a corresponding {@link MetricRecordingSettings} instance.
     * The name hereby acts only as a default for {@link MetricRecordingSettings#getMetric()} and can be overridden by this property.
     * <p>
     * It is possible to directly assign a string or number value to metrics in this map due to the
     * {@link rocks.inspectit.ocelot.config.conversion.NumberToMetricRecordingSettingsConverter} and
     * {@link rocks.inspectit.ocelot.config.conversion.StringToMetricRecordingSettingsConverter}.
     */
    @NotNull
    private Map<@NotBlank String, @Valid MetricRecordingSettings> metrics = Collections.emptyMap();

    /**
     * Stores all configuration options related to tracing.
     */
    @Valid
    private RuleTracingSettings tracing = null;

    /**
     * Validates this rule, invoked by {@link InstrumentationSettings#performValidation(InspectitConfig, ViolationBuilder)}
     *
     * @param container      the root config containing this rule
     * @param definedMetrics all metrics which have been defined in the {@link MetricsSettings}
     * @param vios           the violation builder
     */
    public void performValidation(InstrumentationSettings container, Set<String> definedMetrics, ViolationBuilder vios) {
        checkScopesExist(container, vios);
        checkIncludedRulesExist(container, vios);
        checkMetricRecordingsValid(definedMetrics, vios);
        preEntry.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("preEntry").atProperty(data)));
        entry.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("entry").atProperty(data)));
        postEntry.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("postEntry").atProperty(data)));
        preExit.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("preExit").atProperty(data)));
        exit.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("exit").atProperty(data)));
        postExit.forEach((data, call) -> call.performValidation(container,
                vios.atProperty("postExit").atProperty(data)));
    }

    private void checkMetricRecordingsValid(Set<String> definedMetrics, ViolationBuilder vios) {
        metrics.forEach((name, settings) -> settings.performValidation(name, definedMetrics, vios.atProperty("metrics")));
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


    private void checkIncludedRulesExist(InstrumentationSettings container, ViolationBuilder vios) {
        include.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .filter(name -> !container.getRules().containsKey(name))
                .forEach(name ->
                        vios.message("The included rule '{rule}' does not exist!")
                                .atProperty("include")
                                .parameter("rule", name)
                                .buildAndPublish()
                );
    }

}
