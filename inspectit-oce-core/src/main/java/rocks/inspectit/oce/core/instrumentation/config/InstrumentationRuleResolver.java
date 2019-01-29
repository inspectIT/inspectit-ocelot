package rocks.inspectit.oce.core.instrumentation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InstrumentationRuleResolver {

    @Autowired
    private InstrumentationScopeResolver scopeResolver;

    public Set<InstrumentationRule> resolve(InstrumentationSettings source) {
        if (source.getRules() == null) {
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

    private InstrumentationRule resolveRule(Map<String, InstrumentationScope> scopeMap, Map.Entry<String, InstrumentationRuleSettings> ruleEntry) {
        Set<InstrumentationScope> scopes = ruleEntry.getValue().getScopes().entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .map(scopeMap::get)
                .collect(Collectors.toSet());

        return new InstrumentationRule(ruleEntry.getKey(), scopes);
    }
}
