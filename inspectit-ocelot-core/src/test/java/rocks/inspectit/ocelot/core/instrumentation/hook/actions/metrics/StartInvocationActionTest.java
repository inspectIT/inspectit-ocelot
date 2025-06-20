package rocks.inspectit.ocelot.core.instrumentation.hook.actions.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.metrics.concurrent.ConcurrentInvocationManager;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StartInvocationActionTest {

    @Mock
    ConcurrentInvocationManager concurrentInvocationManager;

    @Mock
    IHookAction.ExecutionContext executionContext;

    final String OPERATION = "operation-name";

    @Test
    void shouldCallManager() {
        StartInvocationAction action = new StartInvocationAction(OPERATION, concurrentInvocationManager);

        action.execute(executionContext);

        verify(concurrentInvocationManager, times(1)).addInvocation(OPERATION);
    }
}
