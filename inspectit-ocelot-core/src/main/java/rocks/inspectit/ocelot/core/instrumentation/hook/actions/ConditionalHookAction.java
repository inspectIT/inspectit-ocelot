package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.ConditionalActionSettings;

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

    /**
     * If a data provider call contains values for the "only-if-..." settings the provider is meant to be only executed conditionally.
     * Therefore in this method we wrap the call in {@link ConditionalHookAction} which check the corresponding preconditions.
     *
     * @param conditions  the condition definition
     * @param inputAction the hook action which does not respect the conditions yet
     * @return the wrapped providerCall in case conditions are defined
     */
    public static IHookAction wrapWithConditionChecks(ConditionalActionSettings conditions, IHookAction inputAction) {
        IHookAction resultAction = inputAction;
        if (!StringUtils.isEmpty(conditions.getOnlyIfTrue())) {
            String conditionDataKey = conditions.getOnlyIfTrue();
            resultAction = new ConditionalHookAction((ctx) -> {
                Object val = ctx.getInspectitContext().getData(conditionDataKey);
                return val != null && (Boolean) val;
            }, resultAction);
        }
        if (!StringUtils.isEmpty(conditions.getOnlyIfFalse())) {
            String conditionDataKey = conditions.getOnlyIfFalse();
            resultAction = new ConditionalHookAction((ctx) -> {
                Object val = ctx.getInspectitContext().getData(conditionDataKey);
                return val != null && !(Boolean) val;
            }, resultAction);
        }

        if (!StringUtils.isEmpty(conditions.getOnlyIfNotNull())) {
            String conditionDataKey = conditions.getOnlyIfNotNull();
            resultAction = new ConditionalHookAction((ctx) -> ctx.getInspectitContext().getData(conditionDataKey) != null, resultAction);
        }
        if (!StringUtils.isEmpty(conditions.getOnlyIfNull())) {
            String conditionDataKey = conditions.getOnlyIfNull();
            resultAction = new ConditionalHookAction((ctx) -> ctx.getInspectitContext().getData(conditionDataKey) == null, resultAction);
        }

        return resultAction;
    }
}
