package rocks.inspectit.oce.core.instrumentation.special;

import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.bootstrap.context.ContextManager;
import rocks.inspectit.oce.core.TestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static org.mockito.Mockito.*;

class ScheduledExecutorContextPropagationSensorTest {

    @Mock
    ContextManager contextManager;

    @Nested
    public class ScheduledExecutorRunnableAdviceTest {

        @Test
        public void verifyContextManagerIsCalled() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
            MockitoAnnotations.initMocks(ScheduledExecutorContextPropagationSensorTest.this);
            Instances.contextManager = contextManager;

            Class<?> adviceClazz = Class.forName("rocks.inspectit.oce.core.instrumentation.special.ScheduledExecutorContextPropagationSensor$ScheduledExecutorRunnableAdvice");
            Method method = TestUtils.resolveMethodsByAnnotation(adviceClazz, Advice.OnMethodEnter.class).get(0);

            Runnable runnable = mock(Runnable.class);

            method.invoke(null, runnable);

            verify(contextManager).wrap(runnable);
            verifyNoMoreInteractions(contextManager);
        }
    }

    @Nested
    public class ScheduledExecutorCallableAdviceTest {

        @Test
        public void verifyContextManagerIsCalled() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
            MockitoAnnotations.initMocks(ScheduledExecutorContextPropagationSensorTest.this);
            Instances.contextManager = contextManager;

            Class<?> adviceClazz = Class.forName("rocks.inspectit.oce.core.instrumentation.special.ScheduledExecutorContextPropagationSensor$ScheduledExecutorCallableAdvice");
            Method method = TestUtils.resolveMethodsByAnnotation(adviceClazz, Advice.OnMethodEnter.class).get(0);

            Callable callable = mock(Callable.class);

            method.invoke(null, callable);

            verify(contextManager).wrap(callable);
            verifyNoMoreInteractions(contextManager);
        }
    }
}