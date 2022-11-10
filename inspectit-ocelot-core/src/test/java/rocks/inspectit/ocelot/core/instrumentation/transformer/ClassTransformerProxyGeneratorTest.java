package rocks.inspectit.ocelot.core.instrumentation.transformer;

import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassTransformerProxyGeneratorTest {

    @Mock
    Instrumentation instrumentation;

    @Mock
    ClassTransformer transformer1;

    @Mock
    ClassTransformer transformer2;

    @InjectMocks
    ClassTransformerProxyGenerator transformerProxy;

    @Nested
    public class Init {

        @Test
        void testProxyCreated() {
            transformerProxy.classTransformers = Arrays.asList(transformer1, transformer2);
            when(transformer1.isEnabled()).thenReturn(true);
            when(transformer2.isEnabled()).thenReturn(false);

            transformerProxy.init();

            assertThat(transformerProxy.activeTransformer).isEqualTo(transformer1);
            assertThat(transformerProxy.transformerProxy).isNotNull();

            verify(instrumentation).addTransformer(transformerProxy.transformerProxy, true);
        }

        @Test
        void testNoTransformerAvailable() {
            transformerProxy.classTransformers = Arrays.asList(transformer1, transformer2);
            when(transformer1.isEnabled()).thenReturn(false);
            when(transformer2.isEnabled()).thenReturn(false);

            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                transformerProxy.init();
            }).withMessage("No active ClassTransformer found!");

        }

        @Test
        void testMultipleActiveTransformerAvailable() {
            transformerProxy.classTransformers = Arrays.asList(transformer1, transformer2);
            when(transformer1.isEnabled()).thenReturn(true);
            when(transformer2.isEnabled()).thenReturn(true);

            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                transformerProxy.init();
            }).withMessageStartingWith("Found more than one active ClassTransformer");

        }
    }

    @Nested
    public class Destroy {

        @Test
        void destroy() {
            transformerProxy.classTransformers = Arrays.asList(transformer1, transformer2);
            when(transformer1.isEnabled()).thenReturn(true);
            when(transformer2.isEnabled()).thenReturn(false);

            transformerProxy.init();

            assertThat(transformerProxy.activeTransformer).isEqualTo(transformer1);
            assertThat(transformerProxy.transformerProxy).isNotNull();

            verify(instrumentation).addTransformer(transformerProxy.transformerProxy, true);

            transformerProxy.destroy();

            verify(instrumentation).removeTransformer(transformerProxy.transformerProxy);

            verify(transformer1).destroy();
        }
    }

    @Nested
    public class ProxyGeneration {

        @Test
        void isProxyForJava8() {
            Assume.assumeThat(System.getProperty("java.version"), CoreMatchers.startsWith("1.8"));

            ClassFileTransformer cft = transformerProxy.createAndInstantiateClassTransformerProxy(transformer1);

            List<Method> transformMethods = Arrays.stream(cft.getClass().getMethods())
                    .filter(m -> m.getName().equals("transform"))
                    .collect(Collectors.toList());

            assertThat(transformMethods.size()).isEqualTo(1);

        }

        @Test
        void isProxyForJava9AndLater() {
            Assume.assumeThat(System.getProperty("java.version"), CoreMatchers.not(CoreMatchers.startsWith("1.8")));

            ClassFileTransformer cft = transformerProxy.createAndInstantiateClassTransformerProxy(transformer1);

            List<Method> transformMethods = Arrays.stream(cft.getClass().getMethods())
                    .filter(m -> m.getName().equals("transform"))
                    .collect(Collectors.toList());

            assertThat(transformMethods.size()).isEqualTo(2);
        }

    }
}