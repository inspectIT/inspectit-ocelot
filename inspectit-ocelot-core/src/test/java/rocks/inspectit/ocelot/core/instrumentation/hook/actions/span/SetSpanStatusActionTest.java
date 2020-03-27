package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Status;
import io.opencensus.trace.Tracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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

    @Mock
    Span span;

    @BeforeEach
    void initMocks() {
        doReturn(ctx).when(executionContext).getInspectitContext();
    }

    @Nested
    class Execute {

        @Test
        void nullStatus() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> null);
            doReturn(true).when(ctx).hasEnteredSpan();

            try (Scope spanScope = Tracing.getTracer().withSpan(span)) {
                action.execute(executionContext);
            }

            verifyZeroInteractions(span);
        }

        @Test
        void falseStatus() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> false);
            doReturn(true).when(ctx).hasEnteredSpan();

            try (Scope spanScope = Tracing.getTracer().withSpan(span)) {
                action.execute(executionContext);
            }

            verifyZeroInteractions(span);
        }


        @Test
        void throwableStatus() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> new Throwable());
            doReturn(true).when(ctx).hasEnteredSpan();

            try (Scope spanScope = Tracing.getTracer().withSpan(span)) {
                action.execute(executionContext);
            }

            verify(span).setStatus(eq(Status.UNKNOWN));
            verify(span).putAttribute(eq("error"), eq(AttributeValue.booleanAttributeValue(true)));
            verifyNoMoreInteractions(span);
        }

        @Test
        void noSpanEntered() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> new Throwable());
            doReturn(false).when(ctx).hasEnteredSpan();

            try (Scope spanScope = Tracing.getTracer().withSpan(span)) {
                action.execute(executionContext);
            }

            verifyZeroInteractions(span);
        }

    }
}
