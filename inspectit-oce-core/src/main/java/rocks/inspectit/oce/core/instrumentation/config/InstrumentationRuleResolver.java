package rocks.inspectit.oce.core.instrumentation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;

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
    public Set<InstrumentationRule> resolve(InstrumentationSettings source) {
        if (CollectionUtils.isEmpty(source.getRules())) {
            return Collections.emptySet();
        }

        Map<String, InstrumentationScope> scopeMap = scopeResolver.resolve(source);

        Set<InstrumentationRule> rules = source.getRules()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isEnabled())
                .map(e -> resolveRule(scopeMap, e))
                .collect(Collectors.toSet());

        return rules;
    }

    /**
     * Creating the {@link InstrumentationRule} instance and linking the scopes to it.
     */
    private InstrumentationRule resolveRule(Map<String, InstrumentationScope> scopeMap, Map.Entry<String, InstrumentationRuleSettings> ruleEntry) {
        Set<InstrumentationScope> scopes = ruleEntry.getValue().getScopes().entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .map(scopeMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return new InstrumentationRule(ruleEntry.getKey(), scopes);
    }
}
