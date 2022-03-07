package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ConditionalActionSettings;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessorFactory;

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
     * If an action contains values for the "only-if-..." settings the provider is meant to be only executed conditionally.
     * Therefore, in this method we wrap the call in {@link ConditionalHookAction} which check the corresponding preconditions.
     *
     * @param conditions              the definitions of the conditions to check
     * @param inputAction             the action to execute only conditionally
     * @param variableAccessorFactory the accessor used for accessing context variables as well as special variables
     * @return the wrapped action in case conditions are defined
     */
    public static IHookAction wrapWithConditionChecks(ConditionalActionSettings conditions, IHookAction inputAction, VariableAccessorFactory variableAccessorFactory) {
        Predicate<ExecutionContext> predicate = getAsPredicate(conditions, variableAccessorFactory);
        if (predicate == ALWAYS_TRUE) {
            return inputAction;
        } else {
            return new ConditionalHookAction(predicate, inputAction);
        }
    }

    /**
     * Returns a predicate for evaluating the given conditions.
     *
     * @param conditions              the conditions to evaluate
     * @param variableAccessorFactory the accessor used for accessing context variables as well as special variables
     * @return the predicate, which is {@link #ALWAYS_TRUE} if no conditions are present
     */
    public static Predicate<ExecutionContext> getAsPredicate(ConditionalActionSettings conditions, VariableAccessorFactory variableAccessorFactory) {
        Predicate<ExecutionContext> result = null;
        if (!StringUtils.isEmpty(conditions.getOnlyIfTrue())) {
            String dataKey = conditions.getOnlyIfTrue();
            VariableAccessor conditionDataAccessor = variableAccessorFactory.getVariableAccessor(dataKey);
            result = and(result, (ctx) -> {
                Object val = conditionDataAccessor.get(ctx);
                return val != null && (Boolean) val;
            });
        }
        if (!StringUtils.isEmpty(conditions.getOnlyIfFalse())) {
            String dataKey = conditions.getOnlyIfFalse();
            VariableAccessor conditionDataAccessor = variableAccessorFactory.getVariableAccessor(dataKey);
            result = and(result, (ctx) -> {
                Object val = conditionDataAccessor.get(ctx);
                return val != null && !(Boolean) val;
            });
        }

        if (!StringUtils.isEmpty(conditions.getOnlyIfNotNull())) {
            String dataKey = conditions.getOnlyIfNotNull();
            VariableAccessor conditionDataAccessor = variableAccessorFactory.getVariableAccessor(dataKey);
            result = and(result, (ctx) -> conditionDataAccessor.get(ctx) != null);
        }
        if (!StringUtils.isEmpty(conditions.getOnlyIfNull())) {
            String dataKey = conditions.getOnlyIfNull();
            VariableAccessor conditionDataAccessor = variableAccessorFactory.getVariableAccessor(dataKey);
            result = and(result, (ctx) -> conditionDataAccessor.get(ctx) == null);
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
