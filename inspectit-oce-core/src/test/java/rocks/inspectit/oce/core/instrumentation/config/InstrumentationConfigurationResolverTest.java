package rocks.inspectit.oce.core.instrumentation.config;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.data.DataSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.oce.core.instrumentation.FakeExecutor;
import rocks.inspectit.oce.core.instrumentation.config.dummy.LambdaTestProvider;
import rocks.inspectit.oce.core.instrumentation.config.model.*;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.testutils.DummyClassLoader;

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

    }


    @Nested
    class GetHookConfigurations {

        final Method testCase_methodA = TestCase.class.getDeclaredMethod("methodA");
        final Method testCase_methodB = TestCase.class.getDeclaredMethod("methodB");

        GetHookConfigurations() throws NoSuchMethodException {
        }

        class TestCase {

            void methodA() {
            }

            void methodB() {
            }
        }

        @Test
        void testTypeMatchingButNoMethodsMatching() throws Exception {
            InstrumentationScope noMethodScope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.none());
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").scope(noMethodScope).build();


            config = InstrumentationConfiguration.builder().source(settings).rule(r1).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(TestCase.class);

            assertThat(result).isEmpty();
            verify(hookResolver, never()).buildHookConfiguration(any(), any(), any());
        }

        @Test
        void testTypeNotMatchingButMethodMatching() throws Exception {
            InstrumentationScope noMethodScope = new InstrumentationScope(ElementMatchers.none(), ElementMatchers.any());
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").scope(noMethodScope).build();


            config = InstrumentationConfiguration.builder().source(settings).rule(r1).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(TestCase.class);

            assertThat(result).isEmpty();
            verify(hookResolver, never()).buildHookConfiguration(any(), any(), any());
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

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(TestCase.class);

            assertThat(result).hasSize(1);
            verify(hookResolver, times(1)).buildHookConfiguration(any(), any(), any());
            verify(hookResolver, times(1))
                    .buildHookConfiguration(eq(testCase_methodA.getDeclaringClass()), argThat(method::matches), eq(Collections.singleton(r1)));
        }


        @Test
        void testMultipleRulesWithSameScopeMatching() throws Exception {
            ElementMatcher.Junction<MethodDescription> method = ElementMatchers.is(testCase_methodA);
            InstrumentationScope methodScope = new InstrumentationScope(ElementMatchers.any(), method);
            InstrumentationRule r1 = InstrumentationRule.builder().name("r1").scope(methodScope).build();
            InstrumentationRule r2 = InstrumentationRule.builder().name("r2").scope(methodScope).build();


            config = InstrumentationConfiguration.builder().source(settings).rule(r1).rule(r2).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", config, true);

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(TestCase.class);

            assertThat(result).hasSize(1);
            verify(hookResolver, times(1)).buildHookConfiguration(any(), any(), any());
            verify(hookResolver, times(1))
                    .buildHookConfiguration(eq(testCase_methodA.getDeclaringClass()), argThat(method::matches), eq(new HashSet<>(Arrays.asList(r1, r2))));
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

            Map<MethodDescription, MethodHookConfiguration> result = resolver.getHookConfigurations(TestCase.class);

            assertThat(result).hasSize(2);
            verify(hookResolver, times(2)).buildHookConfiguration(any(), any(), any());
            verify(hookResolver, times(1))
                    .buildHookConfiguration(eq(testCase_methodA.getDeclaringClass()), argThat(methodA::matches), eq(new HashSet<>(Arrays.asList(r1, r2))));
            verify(hookResolver, times(1))
                    .buildHookConfiguration(eq(testCase_methodB.getDeclaringClass()), argThat(methodB::matches), eq(new HashSet<>(Arrays.asList(r2))));
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
        void ignoreLambdaWithDefaultMethod() throws Exception {
            DummyClassLoader dcl = new DummyClassLoader(getClass().getClassLoader(), LambdaTestProvider.class);
            Class<?> lambdasProvider = dcl.loadClass(LambdaTestProvider.class.getName());

            Class<?> lambdaWithDefault = (Class<?>) lambdasProvider.getMethod("getLambdaWithDefaultMethod").invoke(null);

            assertThat(resolver.isIgnoredClass(lambdaWithDefault, config)).isTrue();
        }

        @Test
        void ignoreLambdaWithInheritedDefaultMethod() throws Exception {
            DummyClassLoader dcl = new DummyClassLoader(getClass().getClassLoader(), LambdaTestProvider.class);
            Class<?> lambdasProvider = dcl.loadClass(LambdaTestProvider.class.getName());

            Class<?> lambdaWithInheritedDefault = (Class<?>) lambdasProvider.getMethod("getLambdaWithInheritedDefaultMethod").invoke(null);

            assertThat(resolver.isIgnoredClass(lambdaWithInheritedDefault, config)).isTrue();
        }

        @Test
        void notIgnoreClassWithDefaultMethod() throws Exception {
            DummyClassLoader dcl = new DummyClassLoader(getClass().getClassLoader(), LambdaTestProvider.class, LambdaTestProvider.getAnonymousClassWithDefaultMethod());
            Class<?> lambdasProvider = dcl.loadClass(LambdaTestProvider.class.getName());

            Class<?> anonWithDefault = (Class<?>) lambdasProvider.getMethod("getAnonymousClassWithDefaultMethod").invoke(null);

            assertThat(resolver.isIgnoredClass(anonWithDefault, config)).isFalse();
        }

        @Test
        void notIgnoreClassWithInheritedDefaultMethod() throws Exception {
            DummyClassLoader dcl = new DummyClassLoader(getClass().getClassLoader(), LambdaTestProvider.class, LambdaTestProvider.getAnonymousClassWithInheritedDefaultMethod());
            Class<?> lambdasProvider = dcl.loadClass(LambdaTestProvider.class.getName());

            Class<?> anonWithInheritedDefault = (Class<?>) lambdasProvider.getMethod("getAnonymousClassWithInheritedDefaultMethod").invoke(null);

            assertThat(resolver.isIgnoredClass(anonWithInheritedDefault, config)).isFalse();
        }

    }


    @Nested
    class ResolveDataProperties {

        InstrumentationSettings testSettings = new InstrumentationSettings();

        @Test
        void defaultSettingsForUnmentionedKeyCorrect() {
            DataProperties dataProps = resolver.resolveDataProperties(testSettings);
            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isFalse();

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isFalse();

            assertThat(dataProps.isTag("my_key")).isTrue();
        }

        @Test
        void noneDownPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setDownPropagation(PropagationMode.NONE);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            DataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isFalse();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isFalse();
        }

        @Test
        void jvmLocalDownPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setDownPropagation(PropagationMode.JVM_LOCAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            DataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isFalse();
        }

        @Test
        void globalDownPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setDownPropagation(PropagationMode.GLOBAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            DataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isTrue();
        }

        @Test
        void noneUpPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setUpPropagation(PropagationMode.NONE);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            DataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isFalse();
        }

        @Test
        void jvmLocalUpPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setUpPropagation(PropagationMode.JVM_LOCAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            DataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isFalse();
        }

        @Test
        void globalUpPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setUpPropagation(PropagationMode.GLOBAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            DataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isTrue();
        }

    }
}

class IgnoredClass implements DoNotInstrumentMarker {

}

class NotIgnoredClass {

}