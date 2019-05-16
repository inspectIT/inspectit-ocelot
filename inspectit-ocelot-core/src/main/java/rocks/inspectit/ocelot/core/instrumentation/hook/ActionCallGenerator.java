package rocks.inspectit.ocelot.core.instrumentation.hook;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.config.utils.ConfigUtils;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.context.ObjectAttachmentsImpl;
import rocks.inspectit.ocelot.core.instrumentation.genericactions.BoundGenericAction;
import rocks.inspectit.ocelot.core.instrumentation.genericactions.GenericActionGenerator;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.ConditionalHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;

@Component
public class ActionCallGenerator {

    @Autowired
    private GenericActionGenerator genericActionGenerator;

    @Autowired
    private ObjectAttachmentsImpl objectAttachments;

    /**
     * Generates a action and binds its arguments.
     *
     * @param methodInfo       the method in which this action will be used.
     * @param actionCallConfig the specification of the call to the data action
     * @return the executable generic action
     */
    public IHookAction generateAndBindGenericAction(MethodReflectionInformation methodInfo, ActionCallConfig actionCallConfig) {
        GenericActionConfig actionConfig = actionCallConfig.getAction();
        val callSettings = actionCallConfig.getCallSettings();
        val injectedActionClass = genericActionGenerator.getOrGenerateGenericAction(actionConfig, methodInfo.getDeclaringClass());

        val dynamicAssignments = getDynamicInputAssignments(methodInfo, actionCallConfig);
        val constantAssignments = getConstantInputAssignments(methodInfo, actionCallConfig);

        IHookAction actionCall = BoundGenericAction.bind(actionCallConfig.getName(), actionConfig, injectedActionClass, constantAssignments, dynamicAssignments);

        return ConditionalHookAction.wrapWithConditionChecks(callSettings, actionCall);
    }

    /**
     * Reads the constant assignments performed by the given action call into a map.
     * The data is immediately converted to the expected input type using a conversion service.
     *
     * @param methodInfo       the method within which the action is executed, used to find the correct types
     * @param actionCallConfig the call whose constant assignments should be queried
     * @return a map mapping the name of the parameters to the constant value they are assigned
     */
    private Map<String, Object> getConstantInputAssignments(MethodReflectionInformation methodInfo, ActionCallConfig actionCallConfig) {
        GenericActionConfig actionConfig = actionCallConfig.getAction();
        Map<String, Object> constantAssignments = new HashMap<>();

        ActionCallSettings callSettings = actionCallConfig.getCallSettings();
        SortedMap<String, String> actionArgumentTypes = actionConfig.getAdditionalArgumentTypes();
        actionCallConfig.getCallSettings().getConstantInput()
                .forEach((argName, value) -> {
                    String expectedTypeName = actionArgumentTypes.get(argName);
                    ClassLoader contextClassloader = methodInfo.getDeclaringClass().getClassLoader();
                    Class<?> expectedValueType = ConfigUtils.locateTypeWithinImports(expectedTypeName, contextClassloader, actionConfig.getImportedPackages());
                    Object convertedValue = callSettings.getConstantInputAsType(argName, expectedValueType);
                    constantAssignments.put(argName, convertedValue);
                });
        if (actionArgumentTypes.containsKey(GenericActionSettings.METHOD_NAME_VARIABLE)) {
            constantAssignments.put(GenericActionSettings.METHOD_NAME_VARIABLE, methodInfo.getName());
        }
        if (actionArgumentTypes.containsKey(GenericActionSettings.OBJECT_ATTACHMENTS_VARIABLE)) {
            constantAssignments.put(GenericActionSettings.OBJECT_ATTACHMENTS_VARIABLE, objectAttachments);
        }
        return constantAssignments;
    }

    /**
     * Reads the dynamic assignments performed by the given action call into a map.
     * Currently the only dynamic assignments are "data-inputs".
     *
     * @param methodInfo       the method within which the action is executed, used to assign special variables
     * @param actionCallConfig the call whose dynamic assignments should be queried
     * @return a map mapping the parameter names to functions which are evaluated during
     * {@link IHookAction#execute(IHookAction.ExecutionContext)}  to find the concrete value for the parameter.
     */
    private Map<String, Function<IHookAction.ExecutionContext, Object>> getDynamicInputAssignments(MethodReflectionInformation methodInfo, ActionCallConfig actionCallConfig) {
        Map<String, Function<IHookAction.ExecutionContext, Object>> dynamicAssignments = new HashMap<>();
        actionCallConfig.getCallSettings().getDataInput()
                .forEach((argName, dataName) ->
                        dynamicAssignments.put(argName, (ctx) -> ctx.getInspectitContext().getData(dataName))
                );
        val additionalInputVars = actionCallConfig.getAction().getAdditionalArgumentTypes().keySet();
        if (additionalInputVars.contains(GenericActionSettings.METHOD_PARAMETER_TYPES_VARIABLE)) {
            dynamicAssignments.put(GenericActionSettings.METHOD_PARAMETER_TYPES_VARIABLE, (ex) -> methodInfo.getParameterTypes());
        }
        if (additionalInputVars.contains(GenericActionSettings.CLASS_VARIABLE)) {
            dynamicAssignments.put(GenericActionSettings.CLASS_VARIABLE, (ex) -> methodInfo.getDeclaringClass());
        }
        if (additionalInputVars.contains(GenericActionSettings.CONTEXT_VARIABLE)) {
            dynamicAssignments.put(GenericActionSettings.CONTEXT_VARIABLE, IHookAction.ExecutionContext::getInspectitContext);
        }
        return dynamicAssignments;
    }
}
