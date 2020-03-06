package rocks.inspectit.ocelot.core.instrumentation.hook;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.testutils.Dummy;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MethodHookGeneratorTest {

    @Mock
    ContextManager contextManager;

    @InjectMocks
    MethodHookGenerator generator;

    @Mock
    VariableAccessorFactory variableAccessorFactory;

    @Nested
    class BuildHook {

        private final TypeDescription dummyType = TypeDescription.ForLoadedType.of(Dummy.class);

        @Test
        void verifyContextManagerProvided() {
            MethodDescription method = dummyType.getDeclaredMethods().stream()
                    .filter(md -> md.getName().equals("doSomething"))
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, method, config);

            Assertions.assertThat(result.getInspectitContextManager()).isSameAs(contextManager);
        }

        @Test
        void verifyConstructorNameAndParameterTypesCorrect() {
            MethodDescription constructor = dummyType.getDeclaredMethods().stream()
                    .filter(MethodDescription::isConstructor)
                    .filter(md -> md.getParameters().size() == 2)
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, constructor, config);

            assertThat(result.getMethodInformation().getName()).isEqualTo("<init>");
            assertThat(result.getMethodInformation().getParameterTypes()).containsExactly(String.class, int.class);
        }

        @Test
        void verifyMethodNameAndParameterTypesCorrect() {
            MethodDescription method = dummyType.getDeclaredMethods().stream()
                    .filter(md -> md.getName().equals("doSomething"))
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, method, config);

            assertThat(result.getMethodInformation().getName()).isEqualTo("doSomething");
            assertThat(result.getMethodInformation().getParameterTypes()).containsExactly(long.class, String.class);
        }

        @Test
        void verifyDeclaringClassCorrect() {
            MethodDescription method = dummyType.getDeclaredMethods().stream()
                    .filter(md -> md.getName().equals("doSomething"))
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, method, config);

            assertThat(result.getMethodInformation().getDeclaringClass()).isSameAs(Dummy.class);
        }
    }

    @Nested
    class BuildMetricAccessor {

        @Test
        public void valueOnly() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(variableAccessorFactory.getConstantAccessor(1D)).thenReturn(mockAccessor);
            MetricRecordingSettings settings = MetricRecordingSettings.builder().metric("name").value("1.0").build();

            MetricAccessor accessor = generator.buildMetricAccessor(settings);

            assertThat(accessor.getVariableAccessor()).isSameAs(mockAccessor);
            assertThat(accessor.getConstantTags()).isEmpty();
            assertThat(accessor.getDataTagAccessors()).isEmpty();
        }

        @Test
        public void dataValueOnly() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(variableAccessorFactory.getVariableAccessor("data-key")).thenReturn(mockAccessor);
            MetricRecordingSettings settings = MetricRecordingSettings.builder().metric("name").value("data-key").build();

            MetricAccessor accessor = generator.buildMetricAccessor(settings);

            assertThat(accessor.getVariableAccessor()).isSameAs(mockAccessor);
            assertThat(accessor.getConstantTags()).isEmpty();
            assertThat(accessor.getDataTagAccessors()).isEmpty();
        }

        @Test
        public void valueAndConstantTag() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(variableAccessorFactory.getConstantAccessor(1D)).thenReturn(mockAccessor);
            MetricRecordingSettings settings = MetricRecordingSettings.builder().metric("name").value("1.0")
                    .constantTags(Collections.singletonMap("tag-key", "tag-key")).build();

            MetricAccessor accessor = generator.buildMetricAccessor(settings);

            assertThat(accessor.getVariableAccessor()).isSameAs(mockAccessor);
            assertThat(accessor.getConstantTags()).containsOnly(entry("tag-key", "tag-key"));
            assertThat(accessor.getDataTagAccessors()).isEmpty();
        }

        @Test
        public void valueAndDataTag() {
            VariableAccessor mockAccessorA = mock(VariableAccessor.class);
            doReturn(mockAccessorA).when(variableAccessorFactory).getConstantAccessor(1D);
            VariableAccessor mockAccessorB = mock(VariableAccessor.class);
            doReturn(mockAccessorB).when(variableAccessorFactory).getVariableAccessor("tag-value");
            MetricRecordingSettings settings = MetricRecordingSettings.builder().metric("name").value("1.0")
                    .dataTags(Collections.singletonMap("tag-key", "tag-value")).build();

            MetricAccessor accessor = generator.buildMetricAccessor(settings);

            assertThat(accessor.getVariableAccessor()).isSameAs(mockAccessorA);
            assertThat(accessor.getConstantTags()).isEmpty();
            assertThat(accessor.getDataTagAccessors()).containsOnly(entry("tag-key", mockAccessorB));
        }
    }
}
