package rocks.inspectit.oce.core.instrumentation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.instrumentation.hook.HookManager;
import rocks.inspectit.oce.core.instrumentation.special.ClassLoaderDelegation;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

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
    HookManager hookManager;

    @InjectMocks
    InstrumentationTriggerer triggerer;

    @Nested
    public class CheckClassesForConfigurationUpdates {

        @Test
        void ensureRequestedClassesRetransformed() throws Exception {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));
            List<Class<?>> classesToInstrument = Arrays.asList(String.class, Character.class);
            when(classLoaderDelegation.makeBootstrapClassesAvailable(any())).thenReturn(true);
            when(classLoaderDelegation.wasBootstrapMadeAvailableTo(any())).thenReturn(true);

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
            when(classLoaderDelegation.makeBootstrapClassesAvailable(any())).thenReturn(true);
            when(classLoaderDelegation.wasBootstrapMadeAvailableTo(any())).thenReturn(true);

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
            when(classLoaderDelegation.wasBootstrapMadeAvailableTo(any())).thenReturn(true);

            doReturn(false).when(instrumentationManager).doesClassRequireRetransformation(any());

            triggerer.checkClassesForConfigurationUpdates(
                    new InstrumentationTriggerer.BatchSize(100, 100));

            assertThat(triggerer.pendingClasses.size()).isEqualTo(0);
            verify(instrumentation, never()).retransformClasses(any());
            verify(classLoaderDelegation, never()).makeBootstrapClassesAvailable(any());
            verify(hookManager, times(TESTING_CLASSES.size())).updateHooksForClass(any());
        }

        @Test
        void ensureNoRetransformCallIfBootstrapUnavailable() throws Exception {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));
            when(classLoaderDelegation.makeBootstrapClassesAvailable(any())).thenReturn(false);
            when(classLoaderDelegation.wasBootstrapMadeAvailableTo(any())).thenReturn(false);

            doReturn(true).when(instrumentationManager).doesClassRequireRetransformation(any());

            triggerer.checkClassesForConfigurationUpdates(
                    new InstrumentationTriggerer.BatchSize(100, 100));

            assertThat(triggerer.pendingClasses.size()).isEqualTo(0);
            verify(instrumentation, never()).retransformClasses(any());
            verify(hookManager, never()).updateHooksForClass(any());
        }
    }

    @Nested
    public class GetBatchOfClassesToRetransform {

        @Test
        void testQueueCapped() {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));
            doReturn(true).when(instrumentationManager).doesClassRequireRetransformation(any());
            when(classLoaderDelegation.makeBootstrapClassesAvailable(any())).thenReturn(true);
            when(classLoaderDelegation.wasBootstrapMadeAvailableTo(any())).thenReturn(true);

            List<Class<?>> classesSelectedForRetransform =
                    triggerer.getBatchOfClassesToRetransform(
                            new InstrumentationTriggerer.BatchSize(100, 100));

            assertThat(classesSelectedForRetransform).containsExactlyInAnyOrder(TESTING_CLASSES.toArray(new Class[]{}));
            assertThat(triggerer.pendingClasses.size()).isEqualTo(0);
        }

        @Test
        void testRetransformationLimitCapped() {
            TESTING_CLASSES.stream().forEach(cl -> triggerer.pendingClasses.put(cl, true));
            doReturn(true).when(instrumentationManager).doesClassRequireRetransformation(any());
            when(classLoaderDelegation.makeBootstrapClassesAvailable(any())).thenReturn(true);
            when(classLoaderDelegation.wasBootstrapMadeAvailableTo(any())).thenReturn(true);

            List<Class<?>> classesSelectedForRetransform =
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
            when(classLoaderDelegation.makeBootstrapClassesAvailable(any())).thenReturn(true);
            when(classLoaderDelegation.wasBootstrapMadeAvailableTo(any())).thenReturn(true);

            List<Class<?>> classesSelectedForRetransform =
                    triggerer.getBatchOfClassesToRetransform(
                            new InstrumentationTriggerer.BatchSize(3, 10));

            assertThat(classesSelectedForRetransform).hasSize(3);
            assertThat(triggerer.pendingClasses.size()).isEqualTo(2);
            assertThat(classesSelectedForRetransform)
                    .doesNotContain(triggerer.pendingClasses.asMap().keySet().toArray(new Class[]{}));
        }
    }
}
