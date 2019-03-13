package rocks.inspectit.oce.core.instrumentation.hook;

import lombok.Value;

@Value
public class ConditionalHookAction implements IHookAction {

    @FunctionalInterface
    public interface Condition {
        boolean evaluate(IHookAction.ExecutionContext context);
    }

    private Condition condition;

    private IHookAction action;

    @Override
    public void execute(ExecutionContext context) {
        if (condition.evaluate(context)) {
            action.execute(context);
        }
    }

    @Override
    public String getName() {
        return action.getName();
    }
}
