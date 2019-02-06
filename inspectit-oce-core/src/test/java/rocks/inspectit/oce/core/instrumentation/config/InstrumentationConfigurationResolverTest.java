package rocks.inspectit.oce.core.instrumentation.config;

import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
            InstrumentationConfiguration configuration = new InstrumentationConfiguration(null, Collections.emptySet());
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
            InstrumentationConfiguration configuration = new InstrumentationConfiguration(null, Collections.singleton(rule));
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
            InstrumentationConfiguration configuration = new InstrumentationConfiguration(null, Collections.singleton(rule));
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
}