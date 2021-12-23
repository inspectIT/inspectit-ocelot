package rocks.inspectit.ocelot.core.selfmonitoring;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionScopeFactoryTest {

    @InjectMocks
    private ActionScopeFactory factory;

    @Mock
    private ActionMetricsRecorder recorder;

    @Nested
    class CreateScope {

        @Mock
        private IHookAction action;

        @Test
        public void enabled() {
            when(recorder.isEnabled()).thenReturn(true);

            IActionScope scope = factory.createScope(action);

            assertThat(scope).isInstanceOf(ActionScopeImpl.class);
        }

        @Test
        public void disabled() {
            when(recorder.isEnabled()).thenReturn(false);

            IActionScope scope = factory.createScope(action);

            assertThat(scope).isSameAs(IActionScope.NOOP_ACTION_SCOPE);
        }

    }
}