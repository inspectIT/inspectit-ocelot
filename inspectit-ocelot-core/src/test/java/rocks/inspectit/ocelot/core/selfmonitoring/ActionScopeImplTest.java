package rocks.inspectit.ocelot.core.selfmonitoring;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActionScopeImplTest {

    @Mock
    private ActionMetricsRecorder recorder;

    @Mock
    private IHookAction action;

    @Nested
    class Close {

        @Test
        public void recordDuration() {
            when(action.getName()).thenReturn("action-name");

            ActionScopeImpl scope = new ActionScopeImpl(action, recorder);
            // wait at least 1 ms so that the verification succeeds.
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            scope.close();

            verify(recorder).record(eq("action-name"), longThat((val) -> val > 0));
            verifyNoMoreInteractions(recorder);
        }

    }
}
