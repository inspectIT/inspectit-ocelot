package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.Tracing;
import lombok.NonNull;
import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.actions.bound.BoundGenericAction;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;

import java.util.Iterator;
import java.util.Map;

/**
 * Action that can be used to wrap another action for which a span should be generated containing various information
 * about the current execution context and the action itself.
 */
@Value(staticConstructor = "wrap")
public class TracingHookAction implements IHookAction {

    /**
     * Prefix used for the span operation name.
     */
    public static final String DEBUG_SPAN_NAME_PREFIX = "*agent* ";

    /**
     * Attribute value for `null` values in span attributes.
     */
    private static final AttributeValue NULL_STRING_ATTRIBUTE = AttributeValue.stringAttributeValue("<NULL>");

    /**
     * Prefix used for span attributes in method hook spans.
     */
    private static final String SPAN_ATTRIBUTE_PREFIX = "inspectit.debug.action.";

    /**
     * The action to decorate/warp.
     */
    @NonNull
    private final IHookAction action;

    /**
     * The configuration of the decorated action.
     */
    private final GenericActionConfig actionConfig;

    /**
     * The name of the rule which defines the decorated action call.
     */
    private final String sourceRuleName;

    @Override
    public void execute(ExecutionContext context) {
        if (!Tracing.getTracer().getCurrentSpan().getContext().isValid()) {
            action.execute(context);
            return;
        }

        String spanName = DEBUG_SPAN_NAME_PREFIX + "action<" + action.getName() + ">";
        SpanBuilder spanBuilder;
        if (context.getMethodHookSpan() == null) {
            spanBuilder = Tracing.getTracer().spanBuilder(spanName);
        } else {
            spanBuilder = Tracing.getTracer().spanBuilderWithExplicitParent(spanName, context.getMethodHookSpan());
        }

        Span span = spanBuilder.startSpan();
        try {
            recordAttribute(span, "bound-method", context.getHook().getMethodInformation().getMethodFQN());
            recordAttribute(span, "name", action.getName());
            recordAttribute(span, "enclosing-rule", sourceRuleName);
            if (actionConfig != null) {
                recordAttribute(span, "is-void", actionConfig.isVoid());
            }

            // method arguments
            Object[] methodArguments = context.getMethodArguments();
            for (int i = 0; i < methodArguments.length; i++) {
                recordAttribute(span, "method-argument." + i, methodArguments[i]);
            }

            // context data
            Iterable<Map.Entry<String, Object>> contextData = context.getInspectitContext().getData();
            for (Map.Entry<String, Object> entry : contextData) {
                recordAttribute(span, "context." + entry.getKey(), entry.getValue());
            }

            if (action instanceof BoundGenericAction) {
                BoundGenericAction boundAction = (BoundGenericAction) this.action;

                recordAttribute(span, "data-key", boundAction.getDataKey());

                // action arguments
                if (actionConfig != null) {
                    Object[] argumentValues = boundAction.getActionArguments(context);
                    Iterator<String> argumentNamesIterator = actionConfig.getActionArgumentTypes().keySet().iterator();
                    for (int i = 0; argumentNamesIterator.hasNext(); i++) {
                        recordAttribute(span, "action-argument." + argumentNamesIterator.next(), argumentValues[i]);
                    }
                }

                // actual call of the wrapped action
                Object actionReturnValue = boundAction.executeImpl(context);

                // action result
                if (actionConfig != null && !actionConfig.isVoid()) {
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
