package rocks.inspectit.ocelot.core.instrumentation.hook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.exposed.ObjectAttachments;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import static rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings.*;

/**
 * Provides a combined view on special variables and data from the {@link rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext}.
 * <p>
 * Given the name of a variable, this component create a function Object for reading the given variable.
 * Why do we return functions and not just have a read(variable,context) method which does the lookup?
 * Returning a function allows the selection of what to return be done when building the hook instead of when it is executed.
 * <p>
 * This avoids having a large switch statement in favor of a simple method invocation.
 */
@Component
@Slf4j
public class VariableAccess {

    @Autowired
    private ObjectAttachments attachments;

    /**
     * Creates a {@link VariableAccessor} for a given fixed variable.
     * If the variable is a special variable (it starts with an underscore), {@link #getSpecialVariableAccessor(String)} will be returned.
     * Otherwise a {@link VariableAccessor} is created which performs a lookup of the given variable in the {@link rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext}.
     *
     * @param variable the name of the variable to create an accessor for
     * @return the {@link VariableAccessor} for the given variable, never null
     */
    public VariableAccessor getVariableAccessor(String variable) {
        if (variable.charAt(0) == '_') {
            VariableAccessor specialVariableAccessor = getSpecialVariableAccessor(variable);
            if (specialVariableAccessor == null) {
                throw new IllegalArgumentException("'" + variable + "' is not a valid special variable!");
            } else {
                return specialVariableAccessor;
            }
        } else {
            return (context) -> context.getInspectitContext().getData(variable);
        }
    }

    /**
     * Creates a {@link VariableAccessor} for the given special variable.
     *
     * @param variable the name of the special variable
     * @return the {@link VariableAccessor} for the given variable or null if "variable" does not denote a special variable
     */
    public VariableAccessor getSpecialVariableAccessor(String variable) {
        switch (variable) {
            case THIS_VARIABLE:
                return IHookAction.ExecutionContext::getThiz;
            case ARGS_VARIABLE:
                return IHookAction.ExecutionContext::getMethodArguments;
            case THROWN_VARIABLE:
                return IHookAction.ExecutionContext::getThrown;
            case RETURN_VALUE_VARIABLE:
                return IHookAction.ExecutionContext::getReturnValue;
            case CLASS_VARIABLE:
                return context -> context.getHook().getMethodInformation().getDeclaringClass();
            case METHOD_NAME_VARIABLE:
                return context -> context.getHook().getMethodInformation().getName();
            case METHOD_PARAMETER_TYPES_VARIABLE:
                return context -> context.getHook().getMethodInformation().getParameterTypes();
            case CONTEXT_VARIABLE:
                return IHookAction.ExecutionContext::getInspectitContext;
            case OBJECT_ATTACHMENTS_VARIABLE:
                return context -> attachments;
        }
        if (variable.startsWith(ARG_VARIABLE_PREFIX)) {
            try {
                int idx = Integer.parseInt(variable.substring(ARG_VARIABLE_PREFIX.length()));
                return context -> context.getMethodArguments()[idx];
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + variable + "' is not a valid argument accessor!", e);
            }
        }
        return null;
    }

    /**
     * Given the name of a special variable, this method returns it's value if it is constant.
     * "Constant" hereby means that the special variable is not runtime-dependent.
     * For example, for a given method hook the values "_methodName" and "_attachments" do never change and therefore are "constant".
     * In contrast "_arg0" for example is dependendent on the current context and therefore NOT constant.
     * <p>
     * It is allowed to store references to these constant variables within the inspectit classloader.
     * This is guaranteed to not cause a memory leak.
     * For this reason "_class" is not a constant special variable, as its storage can cause a memory leak.
     *
     * @param variable      the name of the special variable
     * @param contextMethod the method for which the constant value is being derived
     * @return the value of the special variable if it is a constant, null otherwise
     */
    public Object getConstantSpecialVariable(String variable, MethodReflectionInformation contextMethod) {
        switch (variable) {
            case METHOD_NAME_VARIABLE:
                return contextMethod.getName();
            case OBJECT_ATTACHMENTS_VARIABLE:
                return attachments;
        }
        return null;
    }
}
