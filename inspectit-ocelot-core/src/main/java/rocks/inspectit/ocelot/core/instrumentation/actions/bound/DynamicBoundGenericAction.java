package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Base class for {@link BoundGenericAction}s which have
 * constant values and values depending on the execution context
 * for their input parameters assigned.
 */
public class DynamicBoundGenericAction extends BoundGenericAction {

    /**
     * A template containing the already assigned constant arguments for this generic action.
     * As the same {@link DynamicBoundGenericAction} instance could potentially be used by multiple threads,
     * this array needs to be copied before the dynamicAssignments can be performed.
     */
    private final Object[] argumentsTemplate;

    /**
     * An array containing (a) the index of the addition input to assign and (b) a variable accessor for querying the value.
     * The index corresponds to the index of the parameter in {@link GenericActionConfig#getAdditionalArgumentTypes()}.
     * Therefore the index corresponds to the position in the additionalArguments array with which the
     * {@link IGenericAction#execute(Object[], Object, Object, Throwable, Object[])} function is called.
     */
    private Pair<Integer, VariableAccessor>[] dynamicAssignments;

    DynamicBoundGenericAction(String dataKey, GenericActionConfig actionConfig,
                              InjectedClass<?> action, Map<String, Object> constantAssignments,
                              Map<String, VariableAccessor> dynamicAssignments) {
        super(dataKey, actionConfig, action);

        // the sorted additionalArgumentTypes map defines the number and the order of the additional input
        // parameters the generic action expects
        // therefore we can already reserve the exact amount of space needed for the argumentsTemplate
        int numArgs = actionConfig.getAdditionalArgumentTypes().size();
        argumentsTemplate = new Object[numArgs];

        List<Pair<Integer, VariableAccessor>> dynamicAssignmentsWithIndices = new ArrayList<>();

        //we now loop over the additionalArgumentTypes map and remember the index of the corresponding parameter
        //If the parameter is defined through a constant assignment we simply place it in the argumentsTemplate at the
        //index of the parameter.
        //if the parameter is defined through a dynamic assignment we cannot directly store the value already in the template.
        //Instead we remember the index and the function used to perform the assignment in dynamicAssignments.

        int idx = 0;
        for (String argName : actionConfig.getAdditionalArgumentTypes().keySet()) {
            if (constantAssignments.containsKey(argName)) {
                argumentsTemplate[idx] = constantAssignments.get(argName);
            } else if (dynamicAssignments.containsKey(argName)) {
                dynamicAssignmentsWithIndices.add(Pair.of(idx, dynamicAssignments.get(argName)));
            } else {
                //should never occur as this is validated by config validations
                throw new RuntimeException("Unassigned argument!");
            }
            idx++;
        }
        this.dynamicAssignments = dynamicAssignmentsWithIndices.toArray(new Pair[0]);
    }

    protected Object[] getActionArguments(ExecutionContext context) {
        Object[] args = Arrays.copyOf(argumentsTemplate, argumentsTemplate.length);

        for (val assignment : dynamicAssignments) {
            args[assignment.getLeft()] = assignment.getRight().get(context);
        }
        return args;
    }
}
