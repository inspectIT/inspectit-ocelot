package rocks.inspectit.oce.core.instrumentation.config;

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
import rocks.inspectit.oce.core.instrumentation.config.model.*;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.testutils.DummyClassLoader;

import java.lang.instrument.Instrumentation;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentationConfigurationResolverTest {

    @Mock
    private List<SpecialSensor> specialSensors;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitEnvironment env;

    @Mock
    private Instrumentation instrumentation;

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
            InstrumentationRule rule = new InstrumentationRule("name", Collections.singleton(scope));
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
            InstrumentationRule rule = new InstrumentationRule("name", new HashSet<>(Arrays.asList(scopeA, scopeB)));
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

    }


    @Nested
    class ResolveDataProperties {

        InstrumentationSettings testSettings = new InstrumentationSettings();

        @Test
        void defaultSettingsForUnmentionedKeyCorrect() {
            ResolvedDataProperties dataProps = resolver.resolveDataProperties(testSettings);
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

            ResolvedDataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isFalse();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isFalse();
        }

        @Test
        void jvmLocalDownPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setDownPropagation(PropagationMode.JVM_LOCAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isFalse();
        }

        @Test
        void globalDownPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setDownPropagation(PropagationMode.GLOBAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isTrue();
        }

        @Test
        void noneUpPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setUpPropagation(PropagationMode.NONE);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isFalse();
        }

        @Test
        void jvmLocalUpPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setUpPropagation(PropagationMode.JVM_LOCAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isFalse();
        }

        @Test
        void globalUpPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setUpPropagation(PropagationMode.GLOBAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = resolver.resolveDataProperties(testSettings);

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isTrue();
        }

    }
}

class IgnoredClass implements DoNotInstrumentMarker {

}

class NotIgnoredClass {

}