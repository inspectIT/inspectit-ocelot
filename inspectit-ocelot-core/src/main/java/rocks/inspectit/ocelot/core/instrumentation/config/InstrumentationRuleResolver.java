package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;
import rocks.inspectit.ocelot.config.model.selfmonitoring.ActionTracingMode;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is used to resolve the {@link InstrumentationRule}s which are defined by {@link InstrumentationRuleSettings}
 * contained in the configuration.
 */
@Component
@Slf4j
public class InstrumentationRuleResolver {

    @Autowired
    private InspectitEnvironment environment;

    @Autowired
    private InstrumentationScopeResolver scopeResolver;

    /**
     * Creates a set containing {@link InstrumentationRule}s which are based on the {@link InstrumentationRuleSettings}
     * contained in the given {@link InstrumentationSettings}.
     *
     * @param source the configuration which is used as basis for the rules
     *
     * @return A set containing the resolved rules.
     */
    public Set<InstrumentationRule> resolve(InstrumentationSettings source, Map<String, GenericActionConfig> actions) {
        if (CollectionUtils.isEmpty(source.getRules())) {
            return Collections.emptySet();
        }

        Map<String, InstrumentationScope> scopeMap = scopeResolver.resolve(source);

        Set<InstrumentationRule> rules = source.getRules()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isEnabled())
                .map(e -> resolveRule(e.getKey(), e.getValue(), scopeMap, actions))
                .collect(Collectors.toSet());

        return rules;
    }

    /**
     * Creating the {@link InstrumentationRule} instance and linking the scopes as well as the generic actions to it.
     */
    private InstrumentationRule resolveRule(String name, InstrumentationRuleSettings settings, Map<String, InstrumentationScope> scopeMap, Map<String, GenericActionConfig> actions) {
        val result = InstrumentationRule.builder();
        result.name(name).defaultRule(settings.isDefaultRule());

        boolean hasActionTracing = hasActionTracing(settings);
        result.actionTracing(hasActionTracing);

        settings.getScopes()
                .entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .map(scopeMap::get)
                .filter(Objects::nonNull)
                .forEach(result::scope);

        result.includedRuleNames(settings.getInclude()
                .entrySet()
                .stream()
                .filter(e -> Boolean.TRUE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()));

        settings.getPreEntry()
                .forEach((data, call) -> result.preEntryAction(resolveCall(data, name, call, actions, hasActionTracing)));

        settings.getEntry()
                .forEach((data, call) -> result.entryAction(resolveCall(data, name, call, actions, hasActionTracing)));

        settings.getPostEntry()
                .forEach((data, call) -> result.postEntryAction(resolveCall(data, name, call, actions, hasActionTracing)));

        settings.getPreExit()
                .forEach((data, call) -> result.preExitAction(resolveCall(data, name, call, actions, hasActionTracing)));

        settings.getExit()
                .forEach((data, call) -> result.exitAction(resolveCall(data, name, call, actions, hasActionTracing)));

        settings.getPostExit()
                .forEach((data, call) -> result.postExitAction(resolveCall(data, name, call, actions, hasActionTracing)));

        result.metrics(resolveMetricRecordings(settings));

        result.tracing(settings.getTracing());

        return result.build();
    }

    private boolean hasActionTracing(InstrumentationRuleSettings settings) {
        ActionTracingMode actionTracingMode = environment.getCurrentConfig().getSelfMonitoring().getActionTracing();
        switch (actionTracingMode) {
            case ONLY_ENABLED:
                return settings.isEnableActionTracing();
            case ALL_WITHOUT_DEFAULT:
                return !settings.isDefaultRule();
            case ALL_WITH_DEFAULT:
                return true;
            case OFF:
            default:
                return false;
        }
    }

    @VisibleForTesting
    Multiset<MetricRecordingSettings> resolveMetricRecordings(InstrumentationRuleSettings settings) {
        return settings.getMetrics()
                .entrySet()
                .stream()
                .filter(e -> !StringUtils.isEmpty(e.getValue().getValue()))
                .map(entry -> entry.getValue()
                        .copyWithDefaultMetricName(entry.getKey())) //use map key as default metric name
                .collect(Collectors.toCollection(HashMultiset::create));
    }

    /**
     * Resolves a {@link ActionCallSettings} instance into a {@link ActionCallConfig}.
     * As this involves linking the {@link GenericActionConfig} of the action which is used,
     * the map of known generic actions is required as input.
     *
     * @param dataKey       the key used for storing the action's result in the current context
     * @param ruleName      the name of the rule defining the action call
     * @param call          settings for the action call
     * @param actions       a map mapping the names of data actions to their resolved configuration.
     * @param actionTracing whether action tracing is enabled
     *
     * @return a configuration object describing the action execution
     */
    private ActionCallConfig resolveCall(String dataKey, String ruleName, ActionCallSettings call, Map<String, GenericActionConfig> actions, boolean actionTracing) {
        return ActionCallConfig.builder()
                .dataKey(dataKey)
                .actionTracing(actionTracing)
                .sourceRuleName(ruleName)
                .action(actions.get(call.getAction()))
                .callSettings(call)
                .build();
    }
}
