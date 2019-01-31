package rocks.inspectit.oce.core.instrumentation.config;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.StringMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.*;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        private void setScopeSettings(String scopeKey, TypeScope typeScope, List<MethodMatcherSettings> methodScope, AdvancedScopeSettings advancedSettings) {
            InstrumentationScopeSettings scopeSettings = new InstrumentationScopeSettings();
            scopeSettings.setTypeScope(typeScope);
            scopeSettings.setMethodScope(methodScope);
            scopeSettings.setAdvanced(advancedSettings);
            settings.setScopes(Collections.singletonMap(scopeKey, scopeSettings));
        }

        @Test
        public void nullSettings() {
            Map<String, InstrumentationScope> result = scopeResolver.resolve(null);

            assertThat(result).isEmpty();
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
            setRuleSettings("rule-key", true, Collections.singletonMap("scope-key", true));
            setScopeSettings("scope-key", null, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).isEmpty();
        }

        @Test
        public void ruleWithScope_emptyTypeEmptyMethodEmptyAdvanced() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings classMatcher = new NameMatcherSettings();
            TypeScope typeScope = new TypeScope();
            typeScope.setClasses(Collections.singletonList(classMatcher));
            setScopeSettings(scopeKey, typeScope, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result).flatExtracting(scopeKey).contains(new InstrumentationScope(ElementMatchers.any(), ElementMatchers.any()));
        }

        @Test
        public void ruleWithScope_emptyMethodEmptyAdvanced() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings classMatcher = new NameMatcherSettings();
            classMatcher.setNamePattern("class.Class");
            TypeScope typeScope = new TypeScope();
            typeScope.setClasses(Collections.singletonList(classMatcher));
            setScopeSettings(scopeKey, typeScope, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(ElementMatchers.named("class.Class"), ElementMatchers.any());
        }

        @Test
        public void ruleWithScope_emptyAdvanced() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings classMatcher = new NameMatcherSettings();
            classMatcher.setNamePattern("class.Class");
            TypeScope typeScope = new TypeScope();
            typeScope.setClasses(Collections.singletonList(classMatcher));
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setNamePattern("method");
            setScopeSettings(scopeKey, typeScope, Collections.singletonList(methodSettings), null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(ElementMatchers.named("class.Class"), ElementMatchers.not(ElementMatchers.isConstructor()).and(ElementMatchers.named("method")));
        }

        @Test
        public void ruleWithScope() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings classMatcher = new NameMatcherSettings();
            classMatcher.setNamePattern("class.Class");
            TypeScope typeScope = new TypeScope();
            typeScope.setClasses(Collections.singletonList(classMatcher));
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setNamePattern("method");
            AdvancedScopeSettings advancedScope = new AdvancedScopeSettings();
            advancedScope.setInstrumentOnlyAbstractClasses(true);
            setScopeSettings(scopeKey, typeScope, Collections.singletonList(methodSettings), advancedScope);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(ElementMatchers.named("class.Class").and(ElementMatchers.isAbstract()), ElementMatchers.not(ElementMatchers.isConstructor()).and(ElementMatchers.named("method")));
        }

        @Test
        public void methodMatcherProperties_normalMethod() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            TypeScope typeScope = new TypeScope();
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setVisibility(new AccessModifier[]{AccessModifier.PUBLIC});
            methodSettings.setArguments(new String[0]);
            methodSettings.setMatcherMode(StringMatcher.Mode.MATCHES);
            methodSettings.setNamePattern("method");
            methodSettings.setIsSynchronized(true);
            setScopeSettings(scopeKey, typeScope, Collections.singletonList(methodSettings), null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<MethodDescription> methodMatcher = ElementMatchers
                    .not(ElementMatchers.isConstructor())
                    .and(ElementMatchers.isPublic())
                    .and(ElementMatchers.takesArguments(0))
                    .and(ElementMatchers.nameMatches("method"))
                    .and(ElementMatchers.isSynchronized());

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(ElementMatchers.any(), methodMatcher);
        }

        @Test
        public void methodMatcherProperties_constructor() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            TypeScope typeScope = new TypeScope();
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setIsConstructor(true);
            methodSettings.setVisibility(new AccessModifier[]{AccessModifier.PUBLIC});
            methodSettings.setArguments(new String[]{"any.Class"});
            methodSettings.setMatcherMode(StringMatcher.Mode.MATCHES);
            methodSettings.setNamePattern("method");
            methodSettings.setIsSynchronized(true);
            setScopeSettings(scopeKey, typeScope, Collections.singletonList(methodSettings), null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<MethodDescription> methodMatcher = ElementMatchers
                    .isConstructor()
                    .and(ElementMatchers.isPublic())
                    .and(ElementMatchers.takesArgument(0, ElementMatchers.named("any.Class")));

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(ElementMatchers.any(), methodMatcher);
        }

        @Test
        public void methodMatcherProperties_multipleMethods() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            TypeScope typeScope = new TypeScope();
            MethodMatcherSettings methodSettingsA = new MethodMatcherSettings();
            methodSettingsA.setVisibility(new AccessModifier[]{AccessModifier.PUBLIC, AccessModifier.PROTECTED, AccessModifier.PACKAGE, AccessModifier.PRIVATE});
            methodSettingsA.setNamePattern("methodA");
            MethodMatcherSettings methodSettingsB = new MethodMatcherSettings();
            methodSettingsB.setNamePattern("methodB");
            setScopeSettings(scopeKey, typeScope, Arrays.asList(methodSettingsA, methodSettingsB), null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<MethodDescription> methodMatcher = ElementMatchers
                    .not(ElementMatchers.isConstructor())
                    .and(ElementMatchers.named("methodA"))
                    .or(
                            ElementMatchers.not(ElementMatchers.isConstructor())
                                    .and(ElementMatchers.named("methodB"))
                    );

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(ElementMatchers.any(), methodMatcher);
        }

        @Test
        public void methodMatcherProperties_onlyInherited() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings superMatcher = new NameMatcherSettings();
            superMatcher.setNamePattern("any.Superclass");
            NameMatcherSettings interfaceMatcher = new NameMatcherSettings();
            interfaceMatcher.setNamePattern("any.Interface");
            TypeScope typeScope = new TypeScope();
            typeScope.setSuperclass(superMatcher);
            typeScope.setInterfaces(Collections.singletonList(interfaceMatcher));
            MethodMatcherSettings methodSettings = new MethodMatcherSettings();
            methodSettings.setNamePattern("methodA");
            AdvancedScopeSettings advancedSettings = new AdvancedScopeSettings();
            advancedSettings.setInstrumentOnlyInheritedMethods(true);
            setScopeSettings(scopeKey, typeScope, Collections.singletonList(methodSettings), advancedSettings);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<TypeDescription> typeMatcher = ElementMatchers
                    .hasSuperType(ElementMatchers.named("any.Superclass"))
                    .and(ElementMatchers.hasSuperType(ElementMatchers.named("any.Interface")));

            ElementMatcher.Junction<MethodDescription> methodMatcher = ElementMatchers
                    .isOverriddenFrom(
                            ElementMatchers.named("any.Interface")
                                    .or(ElementMatchers.named("any.Superclass")))
                    .and(
                            ElementMatchers.not(ElementMatchers.isConstructor())
                                    .and(ElementMatchers.named("methodA"))
                    );

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
            superMatcher.setNamePattern("any.Superclass");
            TypeScope typeScope = new TypeScope();
            typeScope.setSuperclass(superMatcher);
            setScopeSettings(scopeKey, typeScope, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<TypeDescription> typeMatcher = ElementMatchers
                    .hasSuperType(ElementMatchers.named("any.Superclass"));

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(typeMatcher, ElementMatchers.any());
        }

        @Test
        public void typeTargets_interfaces() {
            String scopeKey = "scope-key";
            setRuleSettings("rule-key", true, Collections.singletonMap(scopeKey, true));
            NameMatcherSettings superMatcher = new NameMatcherSettings();
            superMatcher.setNamePattern("any.Interface");
            TypeScope typeScope = new TypeScope();
            typeScope.setInterfaces(Collections.singletonList(superMatcher));
            setScopeSettings(scopeKey, typeScope, null, null);

            Map<String, InstrumentationScope> result = scopeResolver.resolve(settings);

            ElementMatcher.Junction<TypeDescription> typeMatcher = ElementMatchers
                    .hasSuperType(ElementMatchers.named("any.Interface"));

            assertThat(result).containsOnlyKeys(scopeKey);
            assertThat(result.get(scopeKey))
                    .extracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(typeMatcher, ElementMatchers.any());
        }
    }
}