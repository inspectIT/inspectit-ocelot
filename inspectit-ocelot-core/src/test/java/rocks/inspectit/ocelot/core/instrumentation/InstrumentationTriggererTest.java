package rocks.inspectit.ocelot.core.instrumentation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.hook.HookManager;
import rocks.inspectit.ocelot.core.instrumentation.special.ClassLoaderDelegation;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstrumentationTriggererTest {


    final List<Class<?>> TESTING_CLASSES = Arrays.asList(
            Integer.class, String.class, Character.class, Long.class, Short.class
    );

    @Mock
    Instrumentation instrumentation;

    @Mock
    SelfMonitoringService selfMonitoring;

    @Mock
    ClassLoaderDelegation classLoaderDelegation;

    @Mock
    InstrumentationManager instrumentationManager;

    @Mock
    InstrumentationConfigurationResolver resolver;

    @Mock
    HookManager hookManager;

    @InjectMocks
    InstrumentationTriggerer triggerer;

    @Nested
    public class CheckClassesForConfigurationUpdates {

        @Test
        void ensureRequestedClassesRetransformed() throws Exception {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));
            List<Class<?>> classesToInstrument = Arrays.asList(String.class, Character.class);
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any())).thenReturn(new LinkedHashSet<>());

            doAnswer((invoc) ->
                    classesToInstrument.contains(invoc.getArgument(0))
            ).when(instrumentationManager).doesClassRequireRetransformation(any());

            triggerer.checkClassesForConfigurationUpdates(
                    new InstrumentationTriggerer.BatchSize(100, 100));

            assertThat(triggerer.pendingClasses.size()).isEqualTo(0);
            ArgumentCaptor<Class> classes = ArgumentCaptor.forClass(Class.class);
            verify(instrumentation, times(1)).retransformClasses(classes.capture());
            assertThat(classesToInstrument).containsExactlyInAnyOrder(classes.getAllValues().toArray(new Class[]{}));
            verify(hookManager, times(TESTING_CLASSES.size())).updateHooksForClass(any());
        }

        @Test
        void ensureTransformationExceptionsHandled() throws Exception {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any())).thenReturn(new LinkedHashSet<>());

            doReturn(true).when(instrumentationManager).doesClassRequireRetransformation(any());

            doThrow(new RuntimeException()).when(instrumentation).retransformClasses(any());

            triggerer.checkClassesForConfigurationUpdates(
                    new InstrumentationTriggerer.BatchSize(100, 100));

            assertThat(triggerer.pendingClasses.size()).isEqualTo(0);
            //the classes are first retransformed in a batch and afterwards retransformed one by one
            verify(instrumentation, times(1 + TESTING_CLASSES.size())).retransformClasses(any());
            for (Class<?> clazz : TESTING_CLASSES) {
                verify(instrumentation, times(1)).retransformClasses(same(clazz));
            }
            verify(hookManager, times(TESTING_CLASSES.size())).updateHooksForClass(any());
        }

        @Test
        void ensureNoRetransformCallIfNotRequired() throws Exception {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));

            doReturn(false).when(instrumentationManager).doesClassRequireRetransformation(any());

            triggerer.checkClassesForConfigurationUpdates(
                    new InstrumentationTriggerer.BatchSize(100, 100));

            assertThat(triggerer.pendingClasses.size()).isEqualTo(0);
            verify(instrumentation, never()).retransformClasses(any());
            verify(hookManager, times(TESTING_CLASSES.size())).updateHooksForClass(any());
        }

        @Test
        void ensureClassLoaderDelegationAppliedFirstInOrder() throws Exception {
            triggerer.pendingClasses.put(Integer.class, true);
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any()))
                    .thenReturn(new LinkedHashSet<>(Arrays.asList(SecureClassLoader.class, URLClassLoader.class)));

            doReturn(true).when(instrumentationManager).doesClassRequireRetransformation(any());

            triggerer.checkClassesForConfigurationUpdates(
                    new InstrumentationTriggerer.BatchSize(100, 100));

            assertThat(triggerer.pendingClasses.size()).isEqualTo(0);
            InOrder ordered = inOrder(instrumentation);
            ordered.verify(instrumentation).retransformClasses(SecureClassLoader.class);
            ordered.verify(instrumentation).retransformClasses(URLClassLoader.class);
            ordered.verify(instrumentation).retransformClasses(Integer.class);
        }
    }

    @Nested
    public class GetBatchOfClassesToRetransform {

        @Test
        void testQueueCapped() {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));
            doReturn(true).when(instrumentationManager).doesClassRequireRetransformation(any());
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any())).thenReturn(new LinkedHashSet<>());

            Set<Class<?>> classesSelectedForRetransform =
                    triggerer.getBatchOfClassesToRetransform(
                            new InstrumentationTriggerer.BatchSize(100, 100));

            assertThat(classesSelectedForRetransform).containsExactlyInAnyOrder(TESTING_CLASSES.toArray(new Class[]{}));
            assertThat(triggerer.pendingClasses.size()).isEqualTo(0);
        }

        @Test
        void testRetransformationLimitCapped() {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));
            doReturn(true).when(instrumentationManager).doesClassRequireRetransformation(any());
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any())).thenReturn(new LinkedHashSet<>());

            Set<Class<?>> classesSelectedForRetransform =
                    triggerer.getBatchOfClassesToRetransform(
                            new InstrumentationTriggerer.BatchSize(5, 2));

            assertThat(classesSelectedForRetransform).hasSize(2);
            assertThat(triggerer.pendingClasses.size()).isEqualTo(3);
            assertThat(classesSelectedForRetransform)
                    .doesNotContain(triggerer.pendingClasses.asMap().keySet().toArray(new Class[]{}));
        }


        @Test
        void testCheckLimitCapped() {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));
            doReturn(true).when(instrumentationManager).doesClassRequireRetransformation(any());
            when(classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(any(), any())).thenReturn(new LinkedHashSet<>());

            Set<Class<?>> classesSelectedForRetransform =
                    triggerer.getBatchOfClassesToRetransform(
                            new InstrumentationTriggerer.BatchSize(3, 10));

            assertThat(classesSelectedForRetransform).hasSize(3);
            assertThat(triggerer.pendingClasses.size()).isEqualTo(2);
            assertThat(classesSelectedForRetransform)
                    .doesNotContain(triggerer.pendingClasses.asMap().keySet().toArray(new Class[]{}));
        }
    }
}
