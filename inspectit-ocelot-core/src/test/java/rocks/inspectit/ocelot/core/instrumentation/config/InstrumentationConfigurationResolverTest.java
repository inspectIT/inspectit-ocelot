package rocks.inspectit.ocelot.core.instrumentation.config;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.FakeExecutor;
import rocks.inspectit.ocelot.core.instrumentation.config.dummy.LambdaTestProvider;
import rocks.inspectit.ocelot.core.instrumentation.config.model.*;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.ocelot.core.testutils.DummyClassLoader;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentationConfigurationResolverTest {

    @Mock
    private List<SpecialSensor> specialSensors;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitEnvironment env;

    @Mock
    private Instrumentation instrumentation;

    @Mock
    MethodHookConfigurationResolver hookResolver;

    @InjectMocks
    private InstrumentationConfigurationResolver resolver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InstrumentationSettings settings;

    InstrumentationConfiguration config;

    @BeforeEach
    void setupInstrumentationMock() {
        lenient().when(instrumentation.isModifiableClass(any())).thenReturn(true);
        lenient().when(settings.getIgnoredBootstrapPackages()).thenReturn(Collections.emptyMap());
        lenient().when(settings.getIgnoredPackages()).thenReturn(Collections.emptyMap());
        lenient().when(settings.isExcludeLambdas()).thenReturn(true);
        lenient().when(settings.isEnabled()).thenReturn(true);

        config = InstrumentationConfiguration.builder().source(settings).build();
    }

    @Nested
    public class GetClassInstrumentationConfiguration {

        @Test
        public void emptyRules() throws IllegalAccessException {
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            ClassInstrumentationConfiguration result = resolver.getClassInstrumentationConfiguration(Object.class);

            assertThat(result).isNotNull();
            assertThat(result.getActiveSpecialSensors()).isEmpty();
            assertThat(result.getActiveRules()).isEmpty();
        }

        @Test
        public void matchingRule() throws IllegalAccessException {
            InstrumentationScope scope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.any());
            InstrumentationRule rule = InstrumentationRule.builder().name("name").scope(scope).build();
            config = InstrumentationConfiguration.builder().source(settings).rule(rule).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            ClassInstrumentationConfiguration result = resolver.getClassInstrumentationConfiguration(Object.class);

            assertThat(result).isNotNull();
            assertThat(result.getActiveSpecialSensors()).isEmpty();
            assertThat(result.getActiveRules())
                    .hasSize(1)
                    .flatExtracting(InstrumentationRule::getScopes)
                    .flatExtracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(ElementMatchers.any(), ElementMatchers.any());
        }

        @Test
        public void narrowingRule() throws IllegalAccessException {
            InstrumentationScope scopeA = new InstrumentationScope(ElementMatchers.nameEndsWithIgnoreCase("object"), ElementMatchers.any());
            InstrumentationScope scopeB = new InstrumentationScope(ElementMatchers.named("not.Matching"), ElementMatchers.any());
            InstrumentationRule rule = InstrumentationRule.builder().name("name").scope(scopeA).scope(scopeB).build();
            config = InstrumentationConfiguration.builder().source(settings).rule(rule).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            ClassInstrumentationConfiguration result = resolver.getClassInstrumentationConfiguration(Object.class);

            assertThat(result).isNotNull();
            assertThat(result.getActiveSpecialSensors()).isEmpty();
            assertThat(result.getActiveRules())
                    .hasSize(1)
                    .flatExtracting(InstrumentationRule::getScopes)
                    .flatExtracting(InstrumentationScope::getTypeMatcher, InstrumentationScope::getMethodMatcher)
                    .containsExactly(ElementMatchers.nameEndsWithIgnoreCase("object"), ElementMatchers.any());
        }

        @Test
        public void disabledInstrumentation() throws IllegalAccessException {
            lenient().when(settings.isEnabled()).thenReturn(false);

            InstrumentationScope scope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.any());
            InstrumentationRule rule = InstrumentationRule.builder().name("name").scope(scope).build();
            config = InstrumentationConfiguration.builder().source(settings).rule(rule).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            ClassInstrumentationConfiguration result = resolver.getClassInstrumentationConfiguration(Object.class);

            assertThat(result).isNotNull();
            assertThat(result).isSameAs(ClassInstrumentationConfiguration.NO_INSTRUMENTATION);
        }
    }


    @Nested
    class GetHookConfigurations {

        final Method testCase_methodA = TestCase.class.getDeclaredMethod("methodA");
        final Method testCase_methodB = TestCase.class.getDeclaredMethod("methodB");

        Class<?> testCaseClass;

        GetHookConfigurations() throws NoSuchMethodException {
        }

        @BeforeEach
        void setupTestCaseClass() throws ClassNotFoundException {
            testCaseClass = Class.forName(TestCase.class.getName(), true, new DummyClassLoader((ClassLoader) null, TestCase.class));
        }

        @Test
        void testTypeMatchingButNoMethodsMatching() throws Exception {
            InstrumentationScope noMethodScope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.none());
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").scope(noMethodScope).build();


            config = InstrumentationConfiguration.builder().source(settings).rule(r1).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(testCaseClass);

            assertThat(result).isEmpty();
            verify(hookResolver, never()).buildHookConfiguration(any(), any());
        }

        @Test
        void testTypeNotMatchingButMethodMatching() throws Exception {
            InstrumentationScope noMethodScope = new InstrumentationScope(ElementMatchers.none(), ElementMatchers.any());
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").scope(noMethodScope).build();


            config = InstrumentationConfiguration.builder().source(settings).rule(r1).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(testCaseClass);

            assertThat(result).isEmpty();
            verify(hookResolver, never()).buildHookConfiguration(any(), any());
        }


        @Test
        void testSingleRuleForMethodMatches() throws Exception {
            ElementMatcher.Junction<MethodDescription> method = ElementMatchers.is(testCase_methodA);
            InstrumentationScope methodScope = new InstrumentationScope(ElementMatchers.any(), method);
            InstrumentationScope noMethodScope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.none());
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").scope(methodScope).build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").scope(noMethodScope).build();


            config = InstrumentationConfiguration.builder().source(settings).rule(r1).rule(r2).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(testCaseClass);

            assertThat(result).hasSize(1);
            verify(hookResolver).buildHookConfiguration(same(config), eq(Collections.singleton(r1)));
            verifyNoMoreInteractions(hookResolver);
        }


        @Test
        void testMultipleRulesWithSameScopeMatching() throws Exception {
            ElementMatcher.Junction<MethodDescription> method = ElementMatchers.is(testCase_methodA);
            InstrumentationScope methodScope = new InstrumentationScope(ElementMatchers.any(), method);
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").scope(methodScope).build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").scope(methodScope).build();


            config = InstrumentationConfiguration.builder().source(settings).rule(r1).rule(r2).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(testCaseClass);

            assertThat(result).hasSize(1);
            verify(hookResolver).buildHookConfiguration(same(config), eq(new HashSet<>(Arrays.asList(r1, r2))));
            verifyNoMoreInteractions(hookResolver);
        }


        @Test
        void testMultipleRulesWithDifferentScopeMatching() throws Exception {
            ElementMatcher.Junction<MethodDescription> methodA = ElementMatchers.is(testCase_methodA);
            ElementMatcher.Junction<MethodDescription> methodB = ElementMatchers.is(testCase_methodB);
            InstrumentationScope methodScope = new InstrumentationScope(ElementMatchers.any(), methodA);
            InstrumentationScope allScope = new InstrumentationScope(ElementMatchers.any(), methodA.or(methodB));
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").scope(methodScope).build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").scope(allScope).build();


            config = InstrumentationConfiguration.builder().source(settings).rule(r1).rule(r2).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(testCaseClass);

            assertThat(result).hasSize(2);
            verify(hookResolver, times(1))
                    .buildHookConfiguration(same(config), eq(new HashSet<>(Arrays.asList(r1, r2))));
            verify(hookResolver, times(1))
                    .buildHookConfiguration(same(config), eq(new HashSet<>(Arrays.asList(r2))));
            verifyNoMoreInteractions(hookResolver);
        }


        @Test
        void testRuleIncludes() throws Exception {
            ElementMatcher.Junction<MethodDescription> method = ElementMatchers.is(testCase_methodA);
            InstrumentationScope methodScope = new InstrumentationScope(ElementMatchers.any(), method);
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").scope(methodScope).includedRuleName("r2").build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").build();


            config = InstrumentationConfiguration.builder().source(settings).rule(r1).rule(r2).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(testCaseClass);

            assertThat(result).hasSize(1);
            verify(hookResolver).buildHookConfiguration(same(config), eq(new HashSet<>(Arrays.asList(r1, r2))));
            verifyNoMoreInteractions(hookResolver);
        }

    }

    @Nested
    class ResolveIncludes {

        @Test
        void noIncludes() {
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").build();
            InstrumentationRule r3 = InstrumentationRule.builder().name("r3").build();

            config = InstrumentationConfiguration.builder().source(settings)
                    .rules(Arrays.asList(r1, r2, r3))
                    .build();

            Set<InstrumentationRule> result = resolver.resolveIncludes(config, Arrays.asList(r1, r2));

            assertThat(result).containsExactlyInAnyOrder(r1, r2);
        }

        @Test
        void singleIncludes() {
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").includedRuleName("r3").build();
            InstrumentationRule r3 = InstrumentationRule.builder().name("r3").build();
            InstrumentationRule r4 = InstrumentationRule.builder().name("r4").build();

            config = InstrumentationConfiguration.builder().source(settings)
                    .rules(Arrays.asList(r1, r2, r3))
                    .build();

            Set<InstrumentationRule> result = resolver.resolveIncludes(config, Arrays.asList(r1, r2));

            assertThat(result).containsExactlyInAnyOrder(r1, r2, r3);
        }


        @Test
        void nonExistingInclude() {
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").includedRuleName("r2").build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").includedRuleName("I don't exist").build();
            InstrumentationRule r3 = InstrumentationRule.builder().name("r3").build();

            config = InstrumentationConfiguration.builder().source(settings)
                    .rules(Arrays.asList(r1, r2, r3))
                    .build();

            Set<InstrumentationRule> result = resolver.resolveIncludes(config, Arrays.asList(r1));

            assertThat(result).containsExactlyInAnyOrder(r1, r2);
        }

        @Test
        void multipleIncludes() {
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").includedRuleName("r2").includedRuleName("r3").build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").build();
            InstrumentationRule r3 = InstrumentationRule.builder().name("r3").build();
            InstrumentationRule r4 = InstrumentationRule.builder().name("r4").build();

            config = InstrumentationConfiguration.builder().source(settings)
                    .rules(Arrays.asList(r1, r2, r3))
                    .build();

            Set<InstrumentationRule> result = resolver.resolveIncludes(config, Arrays.asList(r1));

            assertThat(result).containsExactlyInAnyOrder(r1, r2, r3);
        }

        @Test
        void cyclicDependency() {
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").includedRuleName("r2").build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").includedRuleName("r3").build();
            InstrumentationRule r3 = InstrumentationRule.builder().name("r3").includedRuleName("r1").build();
            InstrumentationRule r4 = InstrumentationRule.builder().name("r4").build();

            config = InstrumentationConfiguration.builder().source(settings)
                    .rules(Arrays.asList(r1, r2, r3))
                    .build();

            Set<InstrumentationRule> result = resolver.resolveIncludes(config, Arrays.asList(r1));

            assertThat(result).containsExactlyInAnyOrder(r1, r2, r3);
        }
    }

    @Nested
    class IsIgnoredClass {

        @Test
        void testNonModifiableCLassesIgnored() {
            when(instrumentation.isModifiableClass(same(String.class))).thenReturn(false);

            assertThat(resolver.isIgnoredClass(String.class, config)).isTrue();
            assertThat(resolver.isIgnoredClass(Integer.class, config)).isFalse();
        }

        @Test
        void testInspectitClassesIgnored() {
            assertThat(resolver.isIgnoredClass(InstrumentationSettings.class, config)).isTrue();
        }


        @Test
        void testBootstrapPackagesIgnoresWork() {
            when(settings.getIgnoredBootstrapPackages()).thenReturn(Collections.singletonMap("java.util.", true));
            assertThat(resolver.isIgnoredClass(Map.class, config)).isTrue();
            assertThat(resolver.isIgnoredClass(String.class, config)).isFalse();
        }

        @Test
        void testGeneralPackagesIgnoresWork() throws Exception {
            DummyClassLoader dcl = new DummyClassLoader(FakeExecutor.class);
            Class<?> copied = Class.forName(FakeExecutor.class.getName(), false, dcl);
            String packagename = copied.getName();
            packagename = packagename.substring(0, packagename.length() - copied.getSimpleName().length());

            assertThat(resolver.isIgnoredClass(copied, config)).isFalse();

            when(settings.getIgnoredPackages()).thenReturn(Collections.singletonMap(packagename, true));

            assertThat(resolver.isIgnoredClass(copied, config)).isTrue();
        }

        @Test
        void testDoNotInstrumentMarkerWorks() throws Exception {
            DummyClassLoader dcl = new DummyClassLoader(getClass().getClassLoader(), IgnoredClass.class, NotIgnoredClass.class);
            Class<?> ignored = Class.forName(IgnoredClass.class.getName(), false, dcl);
            Class<?> notIgnored = Class.forName(NotIgnoredClass.class.getName(), false, dcl);

            assertThat(resolver.isIgnoredClass(ignored, config)).isTrue();
            assertThat(resolver.isIgnoredClass(notIgnored, config)).isFalse();
        }

        @Test
        void ignoreLambda() throws Exception {
            DummyClassLoader dcl = new DummyClassLoader(getClass().getClassLoader(), LambdaTestProvider.class);
            Class<?> lambdasProvider = dcl.loadClass(LambdaTestProvider.class.getName());

            Class<?> lambdaWithDefault = (Class<?>) lambdasProvider.getMethod("getLambdaWithDefaultMethod").invoke(null);

            assertThat(resolver.isIgnoredClass(lambdaWithDefault, config)).isTrue();
        }

        @Test
        void notIgnoreLambda() throws Exception {
            lenient().when(settings.isExcludeLambdas()).thenReturn(false);

            DummyClassLoader dcl = new DummyClassLoader(getClass().getClassLoader(), LambdaTestProvider.class);
            Class<?> lambdasProvider = dcl.loadClass(LambdaTestProvider.class.getName());

            Class<?> lambdaWithDefault = (Class<?>) lambdasProvider.getMethod("getLambdaWithDefaultMethod").invoke(null);

            assertThat(resolver.isIgnoredClass(lambdaWithDefault, config)).isFalse();
        }


        @Test
        void testIgnoredClassloader() throws Exception {
            DummyClassLoader dcl = new IgnoredDummyClassLoader(FakeExecutor.class);
            Class<?> copied = Class.forName(FakeExecutor.class.getName(), false, dcl);
            assertThat(resolver.isIgnoredClass(copied, config)).isTrue();
        }
    }

}

class IgnoredDummyClassLoader extends DummyClassLoader implements DoNotInstrumentMarker {
    public IgnoredDummyClassLoader(Class<?>... classesToCopy) {
        super(classesToCopy);
    }
}

class TestCase {

    void methodA() {
    }

    void methodB() {
    }
}

class IgnoredClass implements DoNotInstrumentMarker {

}

class NotIgnoredClass {

}