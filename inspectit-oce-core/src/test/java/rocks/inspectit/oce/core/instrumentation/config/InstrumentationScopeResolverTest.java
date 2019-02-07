package rocks.inspectit.oce.core.instrumentation.config;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.StringMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.AdvancedScopeSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.MethodMatcherSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.NameMatcherSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static rocks.inspectit.oce.core.config.model.instrumentation.scope.MethodMatcherSettings.AccessModifier;

@ExtendWith(MockitoExtension.class)
class InstrumentationScopeResolverTest {

    @InjectMocks
    private InstrumentationScopeResolver scopeResolver;

    @Nested
    public class Resolve {

        private InstrumentationSettings settings;

        @BeforeEach
        public void beforeEach() {
            settings = new InstrumentationSettings();
        }

        private void setRuleSettings(String ruleKey, boolean enabled, Map<String, Boolean> scopes) {
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setEnabled(enabled);
            ruleSettings.setScopes(scopes);
            settings.setRules(Collections.singletonMap(ruleKey, ruleSettings));
        }

        private void setScopeSettings(String scopeKey, List<NameMatcherSettings> interfaces, NameMatcherSettings superclass, NameMatcherSettings type, List<MethodMatcherSettings> methodScope, AdvancedScopeSettings advancedSettings) {
            InstrumentationScopeSettings scopeSettings = new InstrumentationScopeSettings();
            scopeSettings.setInterfaces(interfaces);
            scopeSettings.setSuperclass(superclass);
            scopeSettings.setType(type);
            scopeSettings.setMethods(methodScope);
            scopeSettings.setAdvanced(advancedSettings);
            settings.setScopes(Collections.singletonMap(scopeKey, scopeSettings));
        }

        @Test
        public void nullRulesAndScopes() {
            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).isEmpty();
        }

        @Test
        public void emptyRulesAndScopes() {
            settings.setRules(Collections.emptyMap());
            settings.setScopes(Collections.emptyMap());

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).isEmpty();
        }

        @Test
        public void ruleWithNoScopes() {
            setRuleSettings("rule-key", true, null);
            settings.setScopes(Collections.emptyMap());

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).isEmpty();
        }

        @Test
        public void ruleWithDisabledScopes() {
            setRuleSettings("rule-key", true, Collections.singletonMap("scope-key", false));
            settings.setScopes(Collections.emptyMap());

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).isEmpty();
        }

        @Test
        public void ruleWithMissingScopes() {
            setRuleSettings("rule-key", true, Collections.singletonMap("scope-key", true));
            settings.setScopes(Collections.emptyMap());

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).isEmpty();
        }

        @Test
        public void ruleWithEmptyScope() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap("scope-key", true));
            setScopeSettings(scopeKey, null, null, null, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).flatExtracting(scopeKey).contains(new InstrumentationScope(any(), any()));
        }

        @Test
        public void ruleWithScope_emptyTypeEmptyMethodEmptyAdvanced() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings classMatcher = new NameMatcherSettings();
            setScopeSettings(scopeKey, null, null, classMatcher, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result).flatExtracting(scopeKey).contains(new InstrumentationScope(any(), any()));
        }

        @Test
        public void ruleWithScope_emptyMethodEmptyAdvanced() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings classMatcher = new NameMatcherSettings();
            classMatcher.setName("class.Class");
            setScopeSettings(scopeKey, null, null, classMatcher, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(named("class.Class"), any());
        }

        @Test
        public void ruleWithScope_emptyAdvanced() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings classMatcher = new NameMatcherSettings();
            classMatcher.setName("class.Class");
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setName("method");
            setScopeSettings(scopeKey, null, null, classMatcher, Collections.singletonList(methodSettings), null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(named("class.Class"), not(isConstructor()).and(named("method")));
        }

        @Test
        public void ruleWithScope() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings classMatcher = new NameMatcherSettings();
            classMatcher.setName("class.Class");
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setName("method");
            AdvancedScopeSettings advancedScope = new AdvancedScopeSettings();
            setScopeSettings(scopeKey, null, null, classMatcher, Collections.singletonList(methodSettings), advancedScope);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(named("class.Class"), not(isConstructor()).and(named("method")));
        }

        @Test
        public void methodMatcherProperties_normalMethod() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setVisibility(Collections.singletonList(AccessModifier.PUBLIC));
            methodSettings.setArguments(Collections.emptyList());
            methodSettings.setMatcherMode(StringMatcher.Mode.MATCHES);
            methodSettings.setName("method");
            methodSettings.setIsSynchronized(true);
            setScopeSettings(scopeKey, null, null, null, Collections.singletonList(methodSettings), null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<MethodDescription> methodMatcher = not(isConstructor())
                    .and(isPublic())
                    .and(takesArguments(0))
                    .and(nameMatches("method"))
                    .and(isSynchronized());

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(any(), methodMatcher);
        }

        @Test
        public void methodMatcherProperties_constructor() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setIsConstructor(true);
            methodSettings.setVisibility(Collections.singletonList(AccessModifier.PUBLIC));
            methodSettings.setArguments(Collections.singletonList("any.Class"));
            methodSettings.setMatcherMode(StringMatcher.Mode.MATCHES);
            methodSettings.setName("method");
            methodSettings.setIsSynchronized(true);
            setScopeSettings(scopeKey, null, null, null, Collections.singletonList(methodSettings), null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<MethodDescription> methodMatcher = isConstructor()
                    .and(isPublic())
                    .and(takesArguments(1).and(takesArgument(0, named("any.Class"))));

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(any(), methodMatcher);
        }

        @Test
        public void methodMatcherProperties_multipleMethods() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            MethodMatcherSettings methodSettingsA = new MethodMatcherSettings();
            methodSettingsA.setName("methodA");
            MethodMatcherSettings methodSettingsB = new MethodMatcherSettings();
            methodSettingsB.setName("methodB");
            setScopeSettings(scopeKey, null, null, null, Arrays.asList(methodSettingsA, methodSettingsB), null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<MethodDescription> methodMatcher = not(isConstructor())
                    .and(named("methodA"))
                    .or(
                            not(isConstructor())
                                    .and(named("methodB"))
                    );

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(any(), methodMatcher);
        }

        @Test
        public void methodMatcherProperties_onlyInherited() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings superMatcher = new NameMatcherSettings();
            superMatcher.setName("any.Superclass");
            NameMatcherSettings interfaceMatcher = new NameMatcherSettings();
            interfaceMatcher.setName("any.Interface");
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setName("methodA");
            AdvancedScopeSettings advancedSettings = new AdvancedScopeSettings();
            advancedSettings.setInstrumentOnlyInheritedMethods(true);
            setScopeSettings(scopeKey, Collections.singletonList(interfaceMatcher), superMatcher, null, Collections.singletonList(methodSettings), advancedSettings);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<TypeDescription> typeMatcher = hasSuperType(not(isInterface()).and(named("any.Superclass")))
                    .and(hasSuperType(isInterface().and(named("any.Interface"))));

            ElementMatcher.Junction<MethodDescription> methodMatcher = not(isConstructor())
                    .and(named("methodA"))
                    .and(isOverriddenFrom(
                            named("any.Interface").and(isInterface())
                                    .or(named("any.Superclass").and(not(isInterface())))
                    ));

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(typeMatcher, methodMatcher);
        }

        @Test
        public void typeTargets_superclass() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings superMatcher = new NameMatcherSettings();
            superMatcher.setName("any.Superclass");
            setScopeSettings(scopeKey, null, superMatcher, null, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<TypeDescription> typeMatcher = hasSuperType(not(isInterface()).and(named("any.Superclass")));

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(typeMatcher, any());
        }

        @Test
        public void typeTargets_interfaces() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings superMatcher = new NameMatcherSettings();
            superMatcher.setName("any.Interface");
            setScopeSettings(scopeKey, Collections.singletonList(superMatcher), null, null, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<TypeDescription> typeMatcher = hasSuperType(isInterface().and(named("any.Interface")));

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(typeMatcher, any());
        }
    }
}