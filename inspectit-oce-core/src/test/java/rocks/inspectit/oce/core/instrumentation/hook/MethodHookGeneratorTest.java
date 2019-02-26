package rocks.inspectit.oce.core.instrumentation.hook;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;
import rocks.inspectit.oce.core.testutils.Dummy;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class MethodHookGeneratorTest {

    @Mock
    ContextManager contextManager;

    @InjectMocks
    MethodHookGenerator generator;

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

            assertThat(result.getInspectitContextManager()).isSameAs(contextManager);
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
}
