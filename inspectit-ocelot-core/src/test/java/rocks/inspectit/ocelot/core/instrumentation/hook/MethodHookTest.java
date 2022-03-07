package rocks.inspectit.ocelot.core.instrumentation.hook;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.selfmonitoring.ActionScopeFactory;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MethodHookTest {

    @Mock
    private InspectitContextImpl context;

    @Mock
    private ContextManager contextManager;

    @Mock
    private MethodReflectionInformation methodInfo;

    @Mock
    private ActionScopeFactory actionScopeFactory;

    @Nested
    class OnEnter {

        @Test
        void ensureRecursionGateReset() {
            // we provoke a NPE
            MethodHook hook = MethodHook.builder().actionScopeFactory(mock(ActionScopeFactory.class)).build();

            assertThat(HookManager.RECURSION_GATE.get()).isFalse();

            // execute hook
            assertThatNullPointerException().isThrownBy(() -> hook.onEnter(null, null));

            assertThat(HookManager.RECURSION_GATE.get()).isFalse();
        }

        @Test
        void testExceptionHandling() {
            when(contextManager.enterNewContext()).thenReturn(context);

            IHookAction first = Mockito.mock(IHookAction.class);
            IHookAction second = Mockito.mock(IHookAction.class);
            IHookAction third = Mockito.mock(IHookAction.class);
            doThrow(Error.class).when(second).execute(any());
            MethodHook hook = MethodHook.builder()
                    .inspectitContextManager(contextManager)
                    .methodInformation(methodInfo)
                    .entryActions(Arrays.asList(first, second, third))
                    .methodInformation(Mockito.mock(MethodReflectionInformation.class))
                    .actionScopeFactory(actionScopeFactory)
                    .build();

            InternalInspectitContext ctx = hook.onEnter(null, null);
            assertThat(ctx).isSameAs(context);
            verify(context, times(1)).makeActive();
            verify(context, times(0)).close();

            verify(first, times(1)).execute(any());
            verify(second, times(1)).execute(any());
            verify(third, times(1)).execute(any());
            verify(actionScopeFactory, times(3)).createScope(any());
            verifyNoMoreInteractions(actionScopeFactory, first, second, third);

            hook.onExit(null, null, null, null, ctx);
            verify(context, times(1)).close();

            ctx = hook.onEnter(null, null);

            verify(first, times(2)).execute(any());
            verify(second, times(1)).execute(any());
            verify(third, times(2)).execute(any());
            verify(actionScopeFactory, times(5)).createScope(any());
            verifyNoMoreInteractions(actionScopeFactory, first, second, third);

            hook.onExit(null, null, null, null, ctx);
        }

        @Test
        void testReactivationOfActionsOnCopy() {
            when(contextManager.enterNewContext()).thenReturn(context);

            IHookAction action = Mockito.mock(IHookAction.class);
            doThrow(Error.class).when(action).execute(any());
            MethodHook hook = MethodHook.builder()
                    .inspectitContextManager(contextManager)
                    .methodInformation(methodInfo)
                    .entryAction(action)
                    .methodInformation(Mockito.mock(MethodReflectionInformation.class))
                    .actionScopeFactory(actionScopeFactory)
                    .build();

            InternalInspectitContext ctx = hook.onEnter(null, null);
            hook.onExit(null, null, null, null, ctx);

            verify(action, times(1)).execute(any());
            verify(actionScopeFactory).createScope(action);
            verifyNoMoreInteractions(actionScopeFactory, action);

            ctx = hook.onEnter(null, null);
            hook.onExit(null, null, null, null, ctx);

            verify(action, times(1)).execute(any());
            verify(actionScopeFactory).createScope(action);
            verifyNoMoreInteractions(actionScopeFactory, action);

            MethodHook copy = hook.getResettedCopy();

            ctx = copy.onEnter(null, null);
            copy.onExit(null, null, null, null, ctx);

            verify(action, times(2)).execute(any());
            verify(actionScopeFactory, times(2)).createScope(action);
            verifyNoMoreInteractions(actionScopeFactory, action);
        }

    }

    @Nested
    class OnExit {

        @Test
        void ensureRecursionGateReset() {
            // we provoke a NPE
            MethodHook hook = MethodHook.builder().actionScopeFactory(mock(ActionScopeFactory.class)).build();

            assertThat(HookManager.RECURSION_GATE.get()).isFalse();

            // execute hook
            assertThatNullPointerException().isThrownBy(() -> hook.onExit(null, null, null, null, null));

            assertThat(HookManager.RECURSION_GATE.get()).isFalse();
        }

        @Test
        void testExceptionHandling() {
            when(contextManager.enterNewContext()).thenReturn(context);

            IHookAction first = Mockito.mock(IHookAction.class);
            IHookAction second = Mockito.mock(IHookAction.class);
            IHookAction third = Mockito.mock(IHookAction.class);
            doThrow(Error.class).when(second).execute(any());
            MethodHook hook = MethodHook.builder()
                    .inspectitContextManager(contextManager)
                    .methodInformation(methodInfo)
                    .exitActions(Arrays.asList(first, second, third))
                    .methodInformation(Mockito.mock(MethodReflectionInformation.class))
                    .actionScopeFactory(actionScopeFactory)
                    .build();

            InternalInspectitContext ctx = hook.onEnter(null, null);
            hook.onExit(null, null, null, null, ctx);

            verify(first, times(1)).execute(any());
            verify(second, times(1)).execute(any());
            verify(third, times(1)).execute(any());
            verify(actionScopeFactory, times(3)).createScope(any());
            verifyNoMoreInteractions(actionScopeFactory, first, second, third);

            ctx = hook.onEnter(null, null);
            hook.onExit(null, null, null, null, ctx);

            verify(first, times(2)).execute(any());
            verify(second, times(1)).execute(any());
            verify(third, times(2)).execute(any());
            verify(actionScopeFactory, times(5)).createScope(any());
            verifyNoMoreInteractions(actionScopeFactory, first, second, third);
        }

        @Test
        void testReactivationOfActionsOnCopy() {
            when(contextManager.enterNewContext()).thenReturn(context);

            IHookAction action = Mockito.mock(IHookAction.class);
            doThrow(Error.class).when(action).execute(any());
            MethodHook hook = MethodHook.builder()
                    .inspectitContextManager(contextManager)
                    .methodInformation(methodInfo)
                    .exitAction(action)
                    .methodInformation(Mockito.mock(MethodReflectionInformation.class))
                    .actionScopeFactory(actionScopeFactory)
                    .build();

            InternalInspectitContext ctx = hook.onEnter(null, null);
            hook.onExit(null, null, null, null, ctx);

            verify(action, times(1)).execute(any());
            verify(actionScopeFactory).createScope(action);
            verifyNoMoreInteractions(actionScopeFactory, action);

            ctx = hook.onEnter(null, null);
            hook.onExit(null, null, null, null, ctx);

            verify(action, times(1)).execute(any());
            verify(actionScopeFactory).createScope(action);
            verifyNoMoreInteractions(actionScopeFactory, action);

            MethodHook copy = hook.getResettedCopy();

            ctx = copy.onEnter(null, null);
            copy.onExit(null, null, null, null, ctx);

            verify(action, times(2)).execute(any());
            verify(actionScopeFactory, times(2)).createScope(action);
            verifyNoMoreInteractions(actionScopeFactory, action);
        }
    }
}
