package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ConditionalActionSettings;

import java.util.Optional;
import java.util.function.Predicate;

@Value
public class ConditionalHookAction implements IHookAction {

    public static final Predicate<ExecutionContext> ALWAYS_TRUE = x -> true;

    private Predicate<ExecutionContext> condition;

    private IHookAction action;

    @Override
    public void execute(ExecutionContext context) {
        if (condition.test(context)) {
            action.execute(context);
        }
    }

    @Override
    public String getName() {
        return action.getName();
    }

    /**
     * If a action contains values for the "only-if-..." settings the provider is meant to be only executed conditionally.
     * Therefore in this method we wrap the call in {@link ConditionalHookAction} which check the corresponding preconditions.
     *
     * @param conditions  the definitions of the conditions to check
     * @param inputAction the action to execute only conditionally
     * @return the wrapped action in case conditions are defined
     */
    public static IHookAction wrapWithConditionChecks(ConditionalActionSettings conditions, IHookAction inputAction) {
        Predicate<ExecutionContext> predicate = getAsPredicate(conditions);
        if (predicate == ALWAYS_TRUE) {
            return inputAction;
        } else {
            return new ConditionalHookAction(predicate, inputAction);
        }
    }

    /**
     * Returns a predicate for evaluating the given conditions.
     *
     * @param conditions the conditions to evaluate
     * @return the predicate, which is {@link #ALWAYS_TRUE} if no conditions are present
     */
    public static Predicate<ExecutionContext> getAsPredicate(ConditionalActionSettings conditions) {
        Predicate<ExecutionContext> result = null;
        if (!StringUtils.isEmpty(conditions.getOnlyIfTrue())) {
            String conditionDataKey = conditions.getOnlyIfTrue();
            result = and(result, (ctx) -> {
                Object val = ctx.getInspectitContext().getData(conditionDataKey);
                return val != null && (Boolean) val;
            });
        }
        if (!StringUtils.isEmpty(conditions.getOnlyIfFalse())) {
            String conditionDataKey = conditions.getOnlyIfFalse();
            result = and(result, (ctx) -> {
                Object val = ctx.getInspectitContext().getData(conditionDataKey);
                return val != null && !(Boolean) val;
            });
        }

        if (!StringUtils.isEmpty(conditions.getOnlyIfNotNull())) {
            String conditionDataKey = conditions.getOnlyIfNotNull();
            result = and(result, (ctx) -> ctx.getInspectitContext().getData(conditionDataKey) != null);
        }
        if (!StringUtils.isEmpty(conditions.getOnlyIfNull())) {
            String conditionDataKey = conditions.getOnlyIfNull();
            result = and(result, (ctx) -> ctx.getInspectitContext().getData(conditionDataKey) == null);
        }
        return Optional.ofNullable(result).orElse(ALWAYS_TRUE);
    }

    private static <T> Predicate<T> and(Predicate<T> first, Predicate<T> second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.and(second);
    }
}
