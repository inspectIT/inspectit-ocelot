package rocks.inspectit.ocelot.core.instrumentation;

import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.InternalSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.ocelot.core.instrumentation.event.ClassInstrumentedEvent;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDefinitionListener;
import rocks.inspectit.ocelot.core.instrumentation.injection.JigsawModuleInstrumenter;
import rocks.inspectit.ocelot.core.instrumentation.special.ClassLoaderDelegation;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.testutils.DummyClassLoader;

import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AsyncClassTransformerTest {

    @Mock
    InspectitEnvironment env;

    @Mock
    ApplicationContext ctx;

    @Mock
    Instrumentation instrumentation;

    @Mock
    SelfMonitoringService selfMonitoring;

    @Mock
    InstrumentationConfigurationResolver configResolver;

    @Mock
    ClassLoaderDelegation classLoaderDelegation;

    @Mock
    JigsawModuleInstrumenter moduleManager;

    @InjectMocks
    AsyncClassTransformer transformer = new AsyncClassTransformer();

    private static byte[] bytecodeOfTest;

    @BeforeAll
    static void readByteCode() {
        bytecodeOfTest = DummyClassLoader.readByteCode(AsyncClassTransformerTest.class);
    }

    @BeforeEach
    void setupTransformer() {
        transformer.classDefinitionListeners = new ArrayList<>();
    }

    @Nested
    public class Init {

        @Test
        void testTransfomerSetup() {
            transformer.init();
            verify(instrumentation).addTransformer(transformer, true);
        }

    }

    @Nested
    public class Destroy {

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
            when(mockSensor.instrument(any(), any(), any())).then(invocation -> invocation.getArgument(2));

            InstrumentationScope scope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.any());
            InstrumentationRule rule = InstrumentationRule.builder().scope(scope).build();

            ClassInstrumentationConfiguration mockedConfig = new ClassInstrumentationConfiguration(
                    Collections.singleton(mockSensor), Collections.singleton(rule), null
            );
            when(configResolver.getClassInstrumentationConfiguration(any())).thenReturn(mockedConfig);
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any()))
                    .thenReturn(new LinkedHashSet<>());

            Class<AsyncClassTransformerTest> clazz = AsyncClassTransformerTest.class;
            String className = clazz.getName().replace('.', '/');
            transformer.transform(clazz.getClassLoader(), className, clazz, null, bytecodeOfTest);

            verify(mockSensor).instrument(any(), any(), any());

            Mockito.reset(mockSensor);
            doAnswer((inv) -> transformer.transform(clazz.getClassLoader(), className, clazz, null, bytecodeOfTest))
                    .when(instrumentation).retransformClasses(clazz);

            transformer.destroy();

            verify(mockSensor, never()).instrument(any(), any(), any());
            verify(instrumentation).retransformClasses(clazz);
            verify(instrumentation).removeTransformer(transformer);
        }


        @Test
        void verifyClassloaderDeinstrumentedLast() throws Exception {

            InternalSettings internalSettings = new InternalSettings();
            internalSettings.setClassRetransformBatchSize(1);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setInternal(internalSettings);
            InspectitConfig conf = new InspectitConfig();
            conf.setInstrumentation(settings);
            when(env.getCurrentConfig()).thenReturn(conf);

            SpecialSensor mockSensor = Mockito.mock(SpecialSensor.class);
            when(mockSensor.instrument(any(), any(), any())).then(invocation -> invocation.getArgument(2));

            ClassInstrumentationConfiguration mockedConfig = new ClassInstrumentationConfiguration(
                    Collections.singleton(mockSensor), Collections.emptySet(), null
            );
            when(configResolver.getClassInstrumentationConfiguration(any())).thenReturn(mockedConfig);
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any()))
                    .thenReturn(new LinkedHashSet<>());

            List<Class<?>> classes = Arrays.asList(String.class, Integer.class, URLClassLoader.class, ClassLoader.class);
            for (Class<?> clazz : classes) {
                transformer.transform(getClass().getClassLoader(), "blub", clazz, null, bytecodeOfTest);
            }

            verify(mockSensor, times(4)).instrument(any(), any(), any());
            reset(instrumentation);

            doAnswer((inv) -> {
                Object[] retransformClasses = inv.getArguments();
                for (Object clazzObj : retransformClasses) {
                    Class<?> clazz = (Class<?>) clazzObj;
                    transformer.transform(clazz.getClassLoader(), "blub", clazz, null, bytecodeOfTest);
                }
                return null;
            }).when(instrumentation).retransformClasses(any());

            transformer.destroy();

            //no new instrumentation
            verify(mockSensor, times(4)).instrument(any(), any(), any());

            //classloaders are deinstrumented last
            ArgumentMatcher<Class> matcher = (x) -> x == ClassLoader.class || x == URLClassLoader.class;

            InOrder ordered = Mockito.inOrder(instrumentation);
            ordered.verify(instrumentation).retransformClasses(Integer.class);
            ordered.verify(instrumentation).retransformClasses(argThat(matcher), argThat(matcher));

            ordered = Mockito.inOrder(instrumentation);
            ordered.verify(instrumentation).retransformClasses(String.class);
            ordered.verify(instrumentation).retransformClasses(argThat(matcher), argThat(matcher));

            verify(instrumentation).removeTransformer(transformer);
        }


        @Test
        void testRetransformErrorHandling() throws Exception {

            InternalSettings internalSettings = new InternalSettings();
            internalSettings.setClassRetransformBatchSize(100);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setInternal(internalSettings);
            InspectitConfig conf = new InspectitConfig();
            conf.setInstrumentation(settings);
            when(env.getCurrentConfig()).thenReturn(conf);

            DummyClassLoader loader = new DummyClassLoader();
            loader.loadCopiesOfClasses(AsyncClassTransformerTest.class, FakeExecutor.class);

            transformer.instrumentedClasses.put(AsyncClassTransformerTest.class, true);
            transformer.instrumentedClasses.put(FakeExecutor.class, true);

            doAnswer(invoc -> {
                Object[] definitons = invoc.getArguments();
                if (Arrays.stream(definitons).anyMatch(c -> c == FakeExecutor.class)) {
                    transformer.instrumentedClasses.invalidate(FakeExecutor.class);
                }
                if (Arrays.stream(definitons).anyMatch(c -> c == AsyncClassTransformerTest.class)) {
                    throw new RuntimeException();
                }
                return null;
            }).when(instrumentation).retransformClasses(any());

            transformer.destroy();

            verify(instrumentation, times(1)).retransformClasses(any(), any());
            verify(instrumentation, times(3)).retransformClasses(any());
        }

    }

    @Nested
    public class Transform {

        @Test
        void verifyClassInstrumentedEventPublished() throws Exception {
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any()))
                    .thenReturn(new LinkedHashSet<>());

            IClassDefinitionListener listener = Mockito.mock(IClassDefinitionListener.class);

            transformer.init();

            SpecialSensor mockSensor = Mockito.mock(SpecialSensor.class);
            when(mockSensor.instrument(any(), any(), any())).then(invocation -> invocation.getArgument(2));
            InstrumentationScope scope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.any());
            InstrumentationRule rule = InstrumentationRule.builder().scope(scope).build();

            ClassInstrumentationConfiguration mockedConfig = new ClassInstrumentationConfiguration(
                    Collections.singleton(mockSensor), Collections.singleton(rule), null
            );
            when(configResolver.getClassInstrumentationConfiguration(any())).thenReturn(mockedConfig);

            Class<AsyncClassTransformerTest> clazz = AsyncClassTransformerTest.class;
            String className = clazz.getName().replace('.', '/');
            transformer.transform(clazz.getClassLoader(), className, getClass(), null, bytecodeOfTest);

            verify(mockSensor).instrument(any(), any(), any());
            verify(ctx).publishEvent(isA(ClassInstrumentedEvent.class));

        }

        @Test
        void testDefinitionListenersInvokedForNewClasses() throws Exception {
            IClassDefinitionListener listener = Mockito.mock(IClassDefinitionListener.class);
            transformer.classDefinitionListeners = Arrays.asList(listener);

            transformer.init();

            Class<AsyncClassTransformerTest> clazz = AsyncClassTransformerTest.class;
            String className = clazz.getName().replace('.', '/');
            ClassLoader loader = clazz.getClassLoader();
            transformer.transform(loader, className, null, null, bytecodeOfTest);

            verify(listener).onNewClassDefined(className, loader);

        }


        @Test
        void testDefinitionListenersNotInvokedForExistingClasses() throws Exception {
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any()))
                    .thenReturn(new LinkedHashSet<>());
            IClassDefinitionListener listener = Mockito.mock(IClassDefinitionListener.class);
            transformer.classDefinitionListeners = Arrays.asList(listener);

            when(configResolver.getClassInstrumentationConfiguration(any()))
                    .thenReturn(ClassInstrumentationConfiguration.NO_INSTRUMENTATION);

            transformer.init();

            Class<AsyncClassTransformerTest> clazz = AsyncClassTransformerTest.class;
            String className = clazz.getName().replace('.', '/');
            ClassLoader loader = clazz.getClassLoader();
            transformer.transform(loader, className, getClass(), null, bytecodeOfTest);

            verify(listener, never()).onNewClassDefined(any(), any());
        }
    }

}
