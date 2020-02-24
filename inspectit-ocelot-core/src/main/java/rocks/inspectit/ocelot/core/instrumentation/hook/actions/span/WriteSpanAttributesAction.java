package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Tracing;
import lombok.AllArgsConstructor;
import lombok.val;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.privacy.obfuscation.IObfuscatory;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Reads specified data keys from the current context and attaches them to the current span as attributes.
 */
@AllArgsConstructor
public class WriteSpanAttributesAction implements IHookAction {

    private final Map<String, VariableAccessor> attributeAccessors;

    private final Supplier<IObfuscatory> obfuscatorySupplier;

    @Override
    public void execute(ExecutionContext context) {
        val span = Tracing.getTracer().getCurrentSpan();
        if (span.getContext().isValid()) {
            for (val entry : attributeAccessors.entrySet()) {
                Object value = entry.getValue().get(context);
                if (value != null) {
                    obfuscatorySupplier.get().putSpanAttribute(span, entry.getKey(), value.toString());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Span Attribute Writing";
    }
}
