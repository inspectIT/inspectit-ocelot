package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.common.Scope;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.privacy.obfuscation.IObfuscatory;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WriteSpanAttributesTest {

    @Mock
    IHookAction.ExecutionContext executionContext;

    @Mock
    InspectitContextImpl inspectitContext;

    @Mock
    IObfuscatory obfuscatory;

    @Spy
    // get the current span. We need to use @Spy instead of @Mock when using the opencensus-shim as mocking the span collides with OTel's implementation of the Span.
    // see also the discussion at https://github.com/inspectIT/inspectit-ocelot/pull/1270
    Span span = Tracing.getTracer().getCurrentSpan();

    @BeforeEach
    void setupMock() {
        doReturn(inspectitContext).when(executionContext).getInspectitContext();
    }

    @Nested
    class Execute {

        @Test
        void verifyNoAttributesWrittenIfNoSpanStarted() {
            doReturn(false).when(inspectitContext).hasEnteredSpan();
            WriteSpanAttributesAction action = WriteSpanAttributesAction.builder()
                    .obfuscatorySupplier(() -> obfuscatory)
                    .attributeAccessor("foo", (exec) -> "bar")
                    .build();

            try (Scope s = Tracing.getTracer().withSpan(span)) {
                action.execute(executionContext);
            }

            verifyNoMoreInteractions(obfuscatory);
        }

        @Test
        void verifyAttributesWritten() {
            doReturn(true).when(inspectitContext).hasEnteredSpan();
            WriteSpanAttributesAction action = WriteSpanAttributesAction.builder()
                    .obfuscatorySupplier(() -> obfuscatory)
                    .attributeAccessor("foo", (exec) -> "bar")
                    .attributeAccessor("hello", (exec) -> 2.0d)
                    .attributeAccessor("iAmNull", (exec) -> null)
                    .build();

            try (Scope s = Tracing.getTracer().withSpan(span)) {
                action.execute(executionContext);
            }

            verify(obfuscatory).putSpanAttribute(same(span), eq("foo"), eq("bar"));
            verify(obfuscatory).putSpanAttribute(same(span), eq("hello"), eq(Double.valueOf(2.0d)));
            verifyNoMoreInteractions(obfuscatory);
        }

    }
}
