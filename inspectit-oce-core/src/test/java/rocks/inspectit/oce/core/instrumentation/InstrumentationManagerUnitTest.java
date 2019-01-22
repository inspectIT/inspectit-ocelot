package rocks.inspectit.oce.core.instrumentation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstrumentationManagerUnitTest {


    final List<Class<?>> TESTING_CLASSES = Arrays.asList(
            Integer.class, String.class, Character.class, Long.class, Short.class
    );

    @Mock
    Instrumentation instrumentation;

    @Mock
    AsyncClassTransformer transformer;

    @Spy
    @InjectMocks
    InstrumentationManager manager;

    @Nested
    public class CheckClassesForConfigurationUpdates {

        @Test
        void ensureRequestedClassesRetransformed() throws Exception {
            when(transformer.isShuttingDown()).thenReturn(false);

            TESTING_CLASSES.stream().forEach(cl -> manager.pendingClasses.put(cl, true));
            List<Class<?>> classesToInstrument = Arrays.asList(String.class, Character.class);

            for (Class<?> clazz : TESTING_CLASSES) {
                doReturn(classesToInstrument.contains(clazz)).when(manager)
                        .doesClassRequireRetransformation(eq(clazz));
            }

            manager.checkClassesForConfigurationUpdates(
                    new InstrumentationManager.BatchSize(100, 100));

            assertThat(manager.pendingClasses.size()).isEqualTo(0);
            ArgumentCaptor<Class> classes = ArgumentCaptor.forClass(Class.class);
            verify(instrumentation, times(1)).retransformClasses(classes.capture());
            assertThat(classesToInstrument).containsExactlyInAnyOrder(classes.getAllValues().toArray(new Class[]{}));
        }

        @Test
        void ensureTransformationExceptionsHandled() throws Exception {
            when(transformer.isShuttingDown()).thenReturn(false);

            TESTING_CLASSES.stream().forEach(cl -> manager.pendingClasses.put(cl, true));


            doReturn(true).when(manager).doesClassRequireRetransformation(any());
            doThrow(new RuntimeException()).when(instrumentation).retransformClasses(any());

            manager.checkClassesForConfigurationUpdates(
                    new InstrumentationManager.BatchSize(100, 100));

            assertThat(manager.pendingClasses.size()).isEqualTo(0);
            verify(instrumentation, times(1)).retransformClasses(any());
        }

        @Test
        void ensureNoRetransformCallIfNotRequired() throws Exception {
            when(transformer.isShuttingDown()).thenReturn(false);

            TESTING_CLASSES.stream().forEach(cl -> manager.pendingClasses.put(cl, true));

            doReturn(false).when(manager).doesClassRequireRetransformation(any());

            manager.checkClassesForConfigurationUpdates(
                    new InstrumentationManager.BatchSize(100, 100));

            assertThat(manager.pendingClasses.size()).isEqualTo(0);
            verify(instrumentation, never()).retransformClasses(any());
        }
    }

    @Nested
    public class GetBatchOfClassesToRetransform {

        @Test
        void testQueueCapped() {
            TESTING_CLASSES.stream().forEach(cl -> manager.pendingClasses.put(cl, true));
            doReturn(true).when(manager).doesClassRequireRetransformation(any());

            List<Class<?>> classesSelectedForRetransform =
                    manager.getBatchOfClassesToRetransform(
                            new InstrumentationManager.BatchSize(100, 100));

            assertThat(classesSelectedForRetransform).containsExactlyInAnyOrder(TESTING_CLASSES.toArray(new Class[]{}));
            assertThat(manager.pendingClasses.size()).isEqualTo(0);
        }

        @Test
        void testRetransformationLimitCapped() {
            TESTING_CLASSES.stream().forEach(cl -> manager.pendingClasses.put(cl, true));
            doReturn(true).when(manager).doesClassRequireRetransformation(any());

            List<Class<?>> classesSelectedForRetransform =
                    manager.getBatchOfClassesToRetransform(
                            new InstrumentationManager.BatchSize(5, 2));

            assertThat(classesSelectedForRetransform).hasSize(2);
            assertThat(manager.pendingClasses.size()).isEqualTo(3);
            assertThat(classesSelectedForRetransform)
                    .doesNotContain(manager.pendingClasses.asMap().keySet().toArray(new Class[]{}));
        }


        @Test
        void testCheckLimitCapped() {
            TESTING_CLASSES.stream().forEach(cl -> manager.pendingClasses.put(cl, true));
            doReturn(true).when(manager).doesClassRequireRetransformation(any());

            List<Class<?>> classesSelectedForRetransform =
                    manager.getBatchOfClassesToRetransform(
                            new InstrumentationManager.BatchSize(3, 10));

            assertThat(classesSelectedForRetransform).hasSize(3);
            assertThat(manager.pendingClasses.size()).isEqualTo(2);
            assertThat(classesSelectedForRetransform)
                    .doesNotContain(manager.pendingClasses.asMap().keySet().toArray(new Class[]{}));
        }
    }
}
