package rocks.inspectit.oce.core.instrumentation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.InternalSettings;
import rocks.inspectit.oce.core.instrumentation.config.ClassInstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AsyncClassTransformerUnitTest {

    @Mock
    InspectitEnvironment env;

    @Mock
    ApplicationContext ctx;

    @Mock
    Instrumentation instrumentation;

    @Mock
    InstrumentationConfigurationResolver configResolver;

    @InjectMocks
    AsyncClassTransformer transformer = new AsyncClassTransformer();

    private static byte[] bytecodeOfTest;

    @BeforeAll
    static void readByteCode() throws Exception {
        InputStream in = AsyncClassTransformerUnitTest.class.getResourceAsStream("AsyncClassTransformerUnitTest.class");
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        while (in.available() > 0) {
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            data.write(buffer, 0, read);
        }
        bytecodeOfTest = data.toByteArray();
    }

    @BeforeEach
    void setupTransformer() {
        transformer.classDefinitionListeners = new ArrayList<>();
    }

    @Test
    void testTransfomerSetup() {
        transformer.init();
        verify(instrumentation, times(1)).addTransformer(transformer, true);
    }

    @Test
    void testTransfomerCleanup() throws Exception {

        InternalSettings internalSettings = new InternalSettings();
        internalSettings.setClassRetransformBatchSize(100);
        InstrumentationSettings settings = new InstrumentationSettings();
        settings.setInternal(internalSettings);
        InspectitConfig conf = new InspectitConfig();
        conf.setInstrumentation(settings);
        when(env.getCurrentConfig()).thenReturn(conf);

        SpecialSensor mockSensor = Mockito.mock(SpecialSensor.class);
        when(mockSensor.shouldInstrument(any(), any())).thenReturn(true);
        when(mockSensor.requiresInstrumentationChange(any(), any(), any())).thenReturn(true);
        when(mockSensor.instrument(any(), any(), any(), any())).then(invocation -> invocation.getArgument(3));
        ClassInstrumentationConfiguration mockedConfig = new ClassInstrumentationConfiguration(
                new HashSet<>(Arrays.asList(mockSensor)), null
        );
        when(configResolver.getClassInstrumentationConfiguration(any())).thenReturn(mockedConfig);

        Class<AsyncClassTransformerUnitTest> clazz = AsyncClassTransformerUnitTest.class;
        String className = clazz.getName().replace('.', '/');
        transformer.transform(clazz.getClassLoader(), className, getClass(), null, bytecodeOfTest);

        verify(mockSensor, times(1)).instrument(any(), any(), any(), any());

        Mockito.reset(mockSensor);
        doAnswer((inv) -> transformer.transform(clazz.getClassLoader(), className, getClass(), null, bytecodeOfTest))
                .when(instrumentation).retransformClasses(clazz);

        transformer.destroy();

        verify(mockSensor, never()).instrument(any(), any(), any(), any());
        verify(instrumentation, times(1)).retransformClasses(clazz);
        verify(instrumentation, times(1)).removeTransformer(transformer);
    }

    @Test
    void veriyClassInstrumentedEventPublished() throws Exception {
        IClassDefinitionListener listener = Mockito.mock(IClassDefinitionListener.class);

        transformer.init();

        SpecialSensor mockSensor = Mockito.mock(SpecialSensor.class);
        when(mockSensor.instrument(any(), any(), any(), any())).then(invocation -> invocation.getArgument(3));
        ClassInstrumentationConfiguration mockedConfig = new ClassInstrumentationConfiguration(
                new HashSet<>(Arrays.asList(mockSensor)), null
        );
        when(configResolver.getClassInstrumentationConfiguration(any())).thenReturn(mockedConfig);

        Class<AsyncClassTransformerUnitTest> clazz = AsyncClassTransformerUnitTest.class;
        String className = clazz.getName().replace('.', '/');
        transformer.transform(clazz.getClassLoader(), className, getClass(), null, bytecodeOfTest);

        verify(mockSensor, times(1)).instrument(any(), any(), any(), any());
        verify(ctx, times(1)).publishEvent(isA(ClassInstrumentedEvent.class));

    }

    @Test
    void testDefinitionListenersInvokedForNewClasses() throws Exception {
        IClassDefinitionListener listener = Mockito.mock(IClassDefinitionListener.class);
        transformer.classDefinitionListeners = Arrays.asList(listener);

        transformer.init();

        Class<AsyncClassTransformerUnitTest> clazz = AsyncClassTransformerUnitTest.class;
        String className = clazz.getName().replace('.', '/');
        ClassLoader loader = clazz.getClassLoader();
        transformer.transform(loader, className, null, null, bytecodeOfTest);

        verify(listener, times(1)).newClassDefined(className, loader);

    }


    @Test
    void testDefinitionListenersNotInvokedForExistingClasses() throws Exception {
        IClassDefinitionListener listener = Mockito.mock(IClassDefinitionListener.class);
        transformer.classDefinitionListeners = Arrays.asList(listener);

        when(configResolver.getClassInstrumentationConfiguration(any()))
                .thenReturn(ClassInstrumentationConfiguration.NO_INSTRUMENTATION);

        transformer.init();

        Class<AsyncClassTransformerUnitTest> clazz = AsyncClassTransformerUnitTest.class;
        String className = clazz.getName().replace('.', '/');
        ClassLoader loader = clazz.getClassLoader();
        transformer.transform(loader, className, getClass(), null, bytecodeOfTest);

        verify(listener, never()).newClassDefined(any(), any());
    }

}
