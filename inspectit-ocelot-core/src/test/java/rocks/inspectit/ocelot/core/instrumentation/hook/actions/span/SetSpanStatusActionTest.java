package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Status;
import io.opencensus.trace.Tracing;
import io.opentelemetry.context.Context;
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
    // get the current span. This is needed when using the opencensus-shim as mocking the span collides with OTel's implementation (OpenTelemetryNoRecordEventsSpanImpl) of the Span.
    // we could also alternatively try to mock SpanImpl such as OpenTelemetrySpanImpl, but I was not able to fix NullpointerExceptions in OpenTelemetrySpanBuilderImpl that way
    Span span = Tracing.getTracer().getCurrentSpan();

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
        // cast the OC span to an OTel span
        io.opentelemetry.api.trace.Span otelSpan = (io.opentelemetry.api.trace.Span) span;
        // verify that only storeInContext has been invoked
        verify(otelSpan).storeInContext(Mockito.any());
        verifyNoMoreInteractions(span);
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

            verifyNoMoreMeaningfulInteractions(span);
        }

        @Test
        void falseStatus() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> false);
            doReturn(true).when(ctx).hasEnteredSpan();

            try (Scope spanScope = Tracing.getTracer().withSpan(span)) {
                action.execute(executionContext);
            }

            verifyNoMoreMeaningfulInteractions(span);
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
            // storeInContext will also be called on the OTel span
            verify((io.opentelemetry.api.trace.Span) (span)).storeInContext(Mockito.any());

            verifyNoMoreInteractions(span);
        }

        @Test
        void noSpanEntered() {
            SetSpanStatusAction action = new SetSpanStatusAction((ctx) -> new Throwable());
            doReturn(false).when(ctx).hasEnteredSpan();

            try (Scope spanScope = Tracing.getTracer().withSpan(span)) {
                action.execute(executionContext);
            }

            verifyNoMoreMeaningfulInteractions(span);
        }

    }
}
