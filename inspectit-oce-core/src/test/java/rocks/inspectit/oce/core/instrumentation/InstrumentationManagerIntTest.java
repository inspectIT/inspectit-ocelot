package rocks.inspectit.oce.core.instrumentation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.oce.core.SpringTestBase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
        "inspectit.instrumentation.internal.inter-batch-delay=1ms" //for faster responses of the test
})
public class InstrumentationManagerIntTest extends SpringTestBase {

    static class DummyClassLoader extends ClassLoader {

        public DummyClassLoader(String classname, byte[] code) {
            super(null);
            defineClass(classname, code, 0, code.length);
        }
    }

    private static Class<?>[] classesWithInstrumentableExecuter;
    private static Class<?>[] classesWithoutInstrumentableExecuter;

    private static Class<?> instrumentableExecutorClass;
    private static Class<?> ignoredExecutor = FakeExecutor.class;
    private static byte[] bytecodeOfInstrumentableExecuter;

    @Autowired
    AsyncClassTransformer transformer;

    @Autowired
    InstrumentationManager manager;


    /**
     * After an invocation of retransformClass for FakeExecuter.class,
     * this holds the result output by the transformer.
     */
    private volatile byte[] transformationResultOfInstrumentableExecuter;

    @BeforeAll
    static void intitializeFakeExecutor() throws Exception {
        InputStream in = AsyncClassTransformerUnitTest.class.getResourceAsStream("FakeExecutor.class");
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        while (in.available() > 0) {
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            data.write(buffer, 0, read);
        }
        bytecodeOfInstrumentableExecuter = data.toByteArray();

        String className = FakeExecutor.class.getName();
        // we need to load the target class from a different classloader because the "this" classloader is ignored
        instrumentableExecutorClass = Class.forName(className, true, new DummyClassLoader(className, bytecodeOfInstrumentableExecuter));


        classesWithInstrumentableExecuter = new Class[]{
                String.class, Integer.class, ignoredExecutor, instrumentableExecutorClass, Comparable.class
        };

        classesWithoutInstrumentableExecuter = new Class[]{
                String.class, Integer.class, ignoredExecutor, Comparable.class
        };
    }

    @BeforeEach
    void resetInstrumentationMock() throws Exception {
        Mockito.reset(mockInstrumentation);
        when(mockInstrumentation.getAllLoadedClasses()).thenReturn(classesWithInstrumentableExecuter);
        when(mockInstrumentation.isModifiableClass(any())).thenReturn(true);
        doAnswer(invoc -> {
            ClassLoader classLoader = instrumentableExecutorClass.getClassLoader();
            String name = instrumentableExecutorClass.getName();
            byte[] result = transformer.transform(classLoader, name, instrumentableExecutorClass, null, bytecodeOfInstrumentableExecuter);
            transformationResultOfInstrumentableExecuter = result;
            return null;
        }).when(mockInstrumentation).retransformClasses(eq(instrumentableExecutorClass));
        doAnswer(invoc -> {
            fail("Instrumentation of a class which should not be instrumeted was triggered!");
            return null;
        }).when(mockInstrumentation).retransformClasses(AdditionalMatchers.not(eq(instrumentableExecutorClass)));
    }

    @Test
    @DirtiesContext
    void testInstrumentationUpdateOnConfigChange() throws Exception {
        transformationResultOfInstrumentableExecuter = null;
        manager.onNewClassesDiscovered(new HashSet<>(Arrays.asList(classesWithInstrumentableExecuter)));

        verify(mockInstrumentation, timeout(5000).times(1))
                .retransformClasses(eq(instrumentableExecutorClass));
        while (transformationResultOfInstrumentableExecuter == null) {
            Thread.sleep(10);
        }

        assertThat(bytecodeOfInstrumentableExecuter).isNotEqualTo(transformationResultOfInstrumentableExecuter);

        transformationResultOfInstrumentableExecuter = null;
        updateProperties(props ->
                props.setProperty("inspectit.instrumentation.special.executor-context-propagation", false)
        );
        verify(mockInstrumentation, timeout(5000).times(2))
                .retransformClasses(eq(instrumentableExecutorClass));
        while (transformationResultOfInstrumentableExecuter == null) {
            Thread.sleep(10);
        }

        assertThat(bytecodeOfInstrumentableExecuter).isEqualTo(transformationResultOfInstrumentableExecuter);

    }


    @Test
    @DirtiesContext
    void testInstrumentationUpdateOnNewClassDiscovery() throws Exception {
        when(mockInstrumentation.getAllLoadedClasses()).thenReturn(classesWithoutInstrumentableExecuter);
        manager.onNewClassesDiscovered(new HashSet<>(Arrays.asList(classesWithoutInstrumentableExecuter)));

        verify(mockInstrumentation, after(1000).never())
                .retransformClasses(eq(instrumentableExecutorClass));

        when(mockInstrumentation.getAllLoadedClasses()).thenReturn(classesWithInstrumentableExecuter);
        manager.onNewClassesDiscovered(new HashSet<>(Arrays.asList(instrumentableExecutorClass)));

        verify(mockInstrumentation, timeout(5000).times(1))
                .retransformClasses(eq(instrumentableExecutorClass));
        while (transformationResultOfInstrumentableExecuter == null) {
            Thread.sleep(10);
        }

        assertThat(bytecodeOfInstrumentableExecuter).isNotEqualTo(transformationResultOfInstrumentableExecuter);

    }
}
