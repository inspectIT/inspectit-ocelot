package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.actions.bound.BoundGenericAction;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;

import java.util.Iterator;

@Value(staticConstructor = "wrap")
public class TracingHookAction implements IHookAction {

    private static final AttributeValue NULL_STRING_ATTRIBUTE = AttributeValue.stringAttributeValue("<NULL>");

    private static final String SPAN_ATTRIBUTE_PREFIX = "inspectit.debug.action.";

    private final IHookAction action;

    private final GenericActionConfig actionConfig;

    @Override
    public void execute(ExecutionContext context) {
        if (!Tracing.getTracer().getCurrentSpan().getContext().isValid()) {
            action.execute(context);
            return;
        }

        String spanName = "Action<" + action.getName() + ">";
        Span span = Tracing.getTracer().spanBuilder(spanName).startSpan();
        try {
            recordAttribute(span, "bound-method", context.getHook().getMethodInformation().getMethodFQN());
            recordAttribute(span, "is-void", actionConfig.isVoid());
            recordAttribute(span, "name", action.getName());

            // method arguments
            Object[] methodArguments = context.getMethodArguments();
            for (int i = 0; i < methodArguments.length; i++) {
                recordAttribute(span, "method-argument." + i, methodArguments[i]);
            }

            if (action instanceof BoundGenericAction) {
                BoundGenericAction boundAction = (BoundGenericAction) this.action;

                recordAttribute(span, "data-key", boundAction.getDataKey());

                // action arguments
                Object[] argumentValues = boundAction.getActionArguments(context);
                Iterator<String> argumentNamesIterator = actionConfig.getActionArgumentTypes().keySet().iterator();

                for (int i = 0; argumentNamesIterator.hasNext(); i++) {
                    recordAttribute(span, "action-argument." + argumentNamesIterator.next(), argumentValues[i]);
                }

                Object actionReturnValue = boundAction.executeImpl(context);

                // action result
                if (!actionConfig.isVoid()) {
                    recordAttribute(span, "return-value", actionReturnValue);
                }
            } else {
                action.execute(context);
            }
        } catch (Throwable throwable) {
            recordAttribute(span, "error", throwable.getMessage());
            throw throwable;
        } finally {
            span.end();
        }
    }

    @Override
    public String getName() {
        return action.getName();
    }

    private void recordAttribute(Span span, String name, Object value) {
        span.putAttribute(SPAN_ATTRIBUTE_PREFIX + name, value != null ? AttributeValue.stringAttributeValue(value.toString()) : NULL_STRING_ATTRIBUTE);
    }
}
