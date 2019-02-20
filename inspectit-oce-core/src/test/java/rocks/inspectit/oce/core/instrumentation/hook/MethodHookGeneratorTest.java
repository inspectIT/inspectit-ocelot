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

import java.lang.reflect.Constructor;

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
        void verifyConstructorSignatureCorrect() {
            MethodDescription constructor = dummyType.getDeclaredMethods().stream()
                    .filter(MethodDescription::isConstructor)
                    .filter(md -> md.getParameters().size() == 2)
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, constructor, config);

            assertThat(result.getMethodName()).isEqualTo("<init>(java.lang.String,int)");
        }

        @Test
        void verifyMethodSignatureCorrect() {
            MethodDescription method = dummyType.getDeclaredMethods().stream()
                    .filter(md -> md.getName().equals("doSomething"))
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, method, config);

            assertThat(result.getMethodName()).isEqualTo("doSomething(long,java.lang.String)");
        }

        @Test
        void verifyReflectionConfigForConstructorCorrect() throws NoSuchMethodException {
            MethodDescription constructor = dummyType.getDeclaredMethods().stream()
                    .filter(MethodDescription::isConstructor)
                    .filter(md -> md.getParameters().size() == 0)
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, constructor, config);

            assertThat(result.getHookedClass()).isSameAs(Dummy.class);
            Constructor<Dummy> declaredConstructor = Dummy.class.getDeclaredConstructor();
            assertThat(result.getHookedConstructor()).isEqualTo(declaredConstructor);
            assertThat(result.getHookedMethod()).isNull();
        }

        @Test
        void verifyReflectionConfigForMethodCorrect() throws Exception {
            MethodDescription method = dummyType.getDeclaredMethods().stream()
                    .filter(md -> md.getName().equals("doSomething"))
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, method, config);

            assertThat(result.getHookedClass()).isSameAs(Dummy.class);
            assertThat(result.getHookedConstructor()).isNull();
            assertThat(result.getHookedMethod()).isEqualTo(Dummy.class.getDeclaredMethod("doSomething", long.class, String.class));
        }

    }
}
