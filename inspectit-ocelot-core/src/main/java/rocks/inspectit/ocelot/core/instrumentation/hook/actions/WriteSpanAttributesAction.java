package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Tracing;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.Map;

/**
 * Reads specified data keys fro mthe current context and attaches them to the current span as attributes.
 */
@AllArgsConstructor
public class WriteSpanAttributesAction implements IHookAction {

    private final Map<String, String> attributes;

    @Override
    public void execute(ExecutionContext context) {
        val span = Tracing.getTracer().getCurrentSpan();
        if (span.getContext().isValid()) {
            val ctx = context.getInspectitContext();
            for (val entry : attributes.entrySet()) {
                Object value = ctx.getData(entry.getValue());
                if (value != null) {
                    span.putAttribute(entry.getKey(), AttributeValue.stringAttributeValue(value.toString()));
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Span Attribute Writing";
    }
}
