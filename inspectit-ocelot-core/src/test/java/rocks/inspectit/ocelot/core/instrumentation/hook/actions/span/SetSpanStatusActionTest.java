package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SetSpanStatusActionTest {

    @Mock
    IHookAction.ExecutionContext executionContext;

    @Mock
    InspectitContextImpl ctx;

    @Spy
    Span span;

    @BeforeEach
    void initMocks() {
        doReturn(ctx).when(executionContext).getInspectitContext();
    }

    /**
     * Verifies that only {@link io.opentelemetry.api.trace.Span#storeInContext(Context)} has been invoked on the given {@link Span}
     *
     * @param span
     */
    static void verifyNoMoreMeaningfulInteractions(Span span) {
        // verify that only makeCurrent and storeInContext has been invoked
        verify(span).makeCurrent();
        verify(span).storeInContext(Mockito.any());
        verifyNoMoreInteractions(span);
    }

    @Nested
    class Execute {

        @Test
        void nullStatus() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> null);
            doReturn(true).when(ctx).hasEnteredSpan();

            try (Scope spanScope = span.makeCurrent()) {
                action.execute(executionContext);
            }

            verifyNoMoreMeaningfulInteractions(span);
        }

        @Test
        void falseStatus() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> false);
            doReturn(true).when(ctx).hasEnteredSpan();

            try (Scope spanScope = span.makeCurrent()) {
                action.execute(executionContext);
            }

            verifyNoMoreMeaningfulInteractions(span);
        }

        @Test
        void throwableStatus() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> new Throwable());
            doReturn(true).when(ctx).hasEnteredSpan();

            try (Scope spanScope = span.makeCurrent()) {
                action.execute(executionContext);
            }

            verify(span).setStatus(eq(StatusCode.ERROR));
            verify(span).setStatus(eq(StatusCode.ERROR), anyString());

            verifyNoMoreMeaningfulInteractions(span);
        }

        @Test
        void noSpanEntered() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> new Throwable());
            doReturn(false).when(ctx).hasEnteredSpan();

            try (Scope spanScope = span.makeCurrent()) {
                action.execute(executionContext);
            }
            verifyNoMoreMeaningfulInteractions(span);
        }

    }
}
