package rocks.inspectit.ocelot.core.instrumentation.special;

import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.IContextManager;
import rocks.inspectit.ocelot.bootstrap.correlation.LogTraceCorrelator;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExecutorContextPropagationSensorTest {

    @Mock
    private IContextManager contextManager;

    @Mock
    private LogTraceCorrelator logTraceCorrelator;

    @BeforeEach
    void beforeEach() {
        Instances.contextManager = contextManager;
        Instances.logTraceCorrelator = logTraceCorrelator;
    }

    @Nested
    public class ExecutorAdvice {

        @Mock
        private Runnable mockRunnable;

        private void invokeTestMethod(Runnable runnable) throws Exception {
            Class<?> adviceClazz = Class.forName("rocks.inspectit.ocelot.core.instrumentation.special.ExecutorContextPropagationSensor$ExecutorAdvice");
            Method method = MethodUtils.getMethodsWithAnnotation(adviceClazz, Advice.OnMethodEnter.class)[0];
            method.invoke(null, runnable);
        }

        @Test
        public void correlationIsInProgress() throws Exception {
            when(contextManager.enterCorrelation()).thenReturn(false);

            invokeTestMethod(mockRunnable);

            verify(contextManager).enterCorrelation();
            verifyNoMoreInteractions(contextManager, logTraceCorrelator);
        }

        @Test
        public void startCorrelationWithLambda() throws Exception {
            when(contextManager.enterCorrelation()).thenReturn(true);
            Runnable wrapLog = mock(Runnable.class);
            when(logTraceCorrelator.wrap(any(Runnable.class))).thenReturn(wrapLog);

            mockRunnable = () -> {
            };

            invokeTestMethod(mockRunnable);

            verify(contextManager).enterCorrelation();
            verify(contextManager).wrap(wrapLog);
            verify(logTraceCorrelator).wrap(mockRunnable);
            verifyNoMoreInteractions(contextManager, logTraceCorrelator);
        }

        @Test
        public void startCorrelationWithNamed() throws Exception {
            when(contextManager.enterCorrelation()).thenReturn(true);

            mockRunnable = new Runnable() {
                @Override
                public void run() {
                }
            };

            invokeTestMethod(mockRunnable);

            verify(contextManager).enterCorrelation();
            verify(contextManager).storeContext(mockRunnable, true);
            verifyNoMoreInteractions(contextManager, logTraceCorrelator);
        }
    }
}
