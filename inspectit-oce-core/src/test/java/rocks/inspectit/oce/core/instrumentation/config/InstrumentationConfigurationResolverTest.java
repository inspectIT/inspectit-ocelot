package rocks.inspectit.oce.core.instrumentation.config;

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
import rocks.inspectit.oce.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.FakeExecutor;
import rocks.inspectit.oce.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.testutils.DummyClassLoader;

import java.lang.instrument.Instrumentation;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
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

    @Nested
    public class GetClassInstrumentationConfiguration {

        @Test
        public void emptyRules() throws IllegalAccessException {
            InstrumentationConfiguration configuration =
                    InstrumentationConfiguration.builder().build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", configuration, true);

            when(instrumentation.isModifiableClass(any())).thenReturn(true);
            when(env.getCurrentConfig().getInstrumentation().getIgnoredBootstrapPackages()).thenReturn(Collections.emptyMap());

            ClassInstrumentationConfiguration result = resolver.getClassInstrumentationConfiguration(Object.class);

            assertThat(result).isNotNull();
            assertThat(result.getActiveSpecialSensors()).isEmpty();
            assertThat(result.getActiveRules()).isEmpty();
        }

        @Test
        public void matchingRule() throws IllegalAccessException {
            InstrumentationScope scope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.any());
            InstrumentationRule rule = new InstrumentationRule("name", Collections.singleton(scope));
            InstrumentationConfiguration configuration = InstrumentationConfiguration.builder().rule(rule).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", configuration, true);

            when(instrumentation.isModifiableClass(any())).thenReturn(true);
            when(env.getCurrentConfig().getInstrumentation().getIgnoredBootstrapPackages()).thenReturn(Collections.emptyMap());

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
            InstrumentationConfiguration configuration = InstrumentationConfiguration.builder().rule(rule).build();
            FieldUtils.writeDeclaredField(resolver, "currentConfig", configuration, true);

            when(instrumentation.isModifiableClass(any())).thenReturn(true);
            when(env.getCurrentConfig().getInstrumentation().getIgnoredBootstrapPackages()).thenReturn(Collections.emptyMap());

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


        @Mock(answer = Answers.RETURNS_DEEP_STUBS)
        InstrumentationSettings settings;

        InstrumentationConfiguration config;

        @BeforeEach
        void setupInstrumentationMock() {
            when(instrumentation.isModifiableClass(any())).thenReturn(true);
            when(settings.getIgnoredBootstrapPackages()).thenReturn(Collections.emptyMap());
            when(settings.getIgnoredPackages()).thenReturn(Collections.emptyMap());

            config = InstrumentationConfiguration.builder().source(settings).build();
        }

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
}

class IgnoredClass implements DoNotInstrumentMarker {

}

class NotIgnoredClass {

}