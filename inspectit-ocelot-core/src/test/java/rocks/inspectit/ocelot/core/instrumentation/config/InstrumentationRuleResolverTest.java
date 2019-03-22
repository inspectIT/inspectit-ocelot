package rocks.inspectit.ocelot.core.instrumentation.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentationRuleResolverTest {

    @Mock
    private InstrumentationScopeResolver scopeResolver;

    @InjectMocks
    private InstrumentationRuleResolver ruleResolver;

    @Nested
    public class Resolve {

        @Test
        public void emptySettings() {
            InstrumentationSettings settings = new InstrumentationSettings();

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).isEmpty();
        }

        @Test
        public void resolveRulesNoScopes() {
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setScopes(Collections.singletonMap("scope-key", true));
            ruleSettings.setEnabled(true);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            when(scopeResolver.resolve(settings)).thenReturn(Collections.emptyMap());

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getName).contains("rule-key");
            assertThat(result).flatExtracting(InstrumentationRule::getScopes).isEmpty();
            verify(scopeResolver).resolve(settings);
            verifyNoMoreInteractions(scopeResolver);
        }

        @Test
        public void resolveRules() {
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setScopes(Collections.singletonMap("scope-key", true));
            ruleSettings.setEnabled(true);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            InstrumentationScope scope = new InstrumentationScope(null, null);

            when(scopeResolver.resolve(settings)).thenReturn(Collections.singletonMap("scope-key", scope));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getName).contains("rule-key");
            assertThat(result).flatExtracting(InstrumentationRule::getScopes).contains(scope);
            verify(scopeResolver).resolve(settings);
            verifyNoMoreInteractions(scopeResolver);
        }

        @Test
        public void resolveRulesDisabledScope() {
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setScopes(Collections.singletonMap("scope-key", false));
            ruleSettings.setEnabled(true);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            InstrumentationScope scope = new InstrumentationScope(null, null);

            when(scopeResolver.resolve(settings)).thenReturn(Collections.singletonMap("scope-key", scope));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getName).contains("rule-key");
            assertThat(result).flatExtracting(InstrumentationRule::getScopes).isEmpty();
            verify(scopeResolver).resolve(settings);
            verifyNoMoreInteractions(scopeResolver);
        }

    }

}