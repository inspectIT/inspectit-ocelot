package rocks.inspectit.ocelot.core.instrumentation.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
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
    private InstrumentationScopeResolver scopeResolver;

    /**
     * Creates a set containing {@link InstrumentationRule}s which are based on the {@link InstrumentationRuleSettings}
     * contained in the given {@link InstrumentationSettings}.
     *
     * @param source the configuration which is used as basis for the rules
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
        result.name(name);
        settings.getScopes().entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .map(scopeMap::get)
                .filter(Objects::nonNull)
                .forEach(result::scope);

        settings.getEntry().forEach((data, call) ->
                result.entryAction(resolveCall(data, call, actions))
        );

        settings.getExit().forEach((data, call) ->
                result.exitAction(resolveCall(data, call, actions))
        );

        settings.getMetrics().entrySet().stream()
                .filter(e -> !StringUtils.isEmpty(e.getValue()))
                .forEach(e -> result.metric(e.getKey(), e.getValue()));

        result.tracing(settings.getTracing());

        return result.build();
    }

    /**
     * Resolves a {@link ActionCallSettings} instance into a {@link ActionCallConfig}.
     * As this involves linking the {@link GenericActionConfig} of the action which is used,
     * the map of known generic actions is required as input.
     *
     * @param name    the name used for the call, corresponds to the written data key for action calls
     * @param actions a map mapping the names of data actions to their resolved configuration.
     * @param call
     * @return
     */
    private ActionCallConfig resolveCall(String name, ActionCallSettings call, Map<String, GenericActionConfig> actions) {
        return ActionCallConfig.builder()
                .name(name)
                .action(actions.get(call.getAction()))
                .callSettings(call)
                .build();
    }
}
