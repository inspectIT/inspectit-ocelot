package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opentelemetry.api.trace.Span;
import lombok.*;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.privacy.obfuscation.IObfuscatory;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Reads specified data keys from the current context and attaches them to the current span as attributes.
 */
@AllArgsConstructor
@Builder
public class WriteSpanAttributesAction implements IHookAction {

    @Singular
    private final Map<String, VariableAccessor> attributeAccessors;

    private final Supplier<IObfuscatory> obfuscatorySupplier;

    @Override
    public void execute(ExecutionContext context) {
        if (context.getInspectitContext().hasEnteredSpan()) {
            Span span = Span.current();
            for (val entry : attributeAccessors.entrySet()) {
                Object value = entry.getValue().get(context);
                if (value != null) {
                    obfuscatorySupplier.get().putSpanAttribute(span, entry.getKey(), value);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Span attribute writing";
    }
}
