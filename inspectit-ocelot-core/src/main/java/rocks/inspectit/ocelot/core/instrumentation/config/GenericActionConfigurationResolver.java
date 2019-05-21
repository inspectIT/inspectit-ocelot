package rocks.inspectit.ocelot.core.instrumentation.config;

import lombok.val;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;

import java.util.HashMap;
import java.util.Map;

@Component
public class GenericActionConfigurationResolver {

    /**
     * Returns a map mapping the names of generic actions to their resolved configurations
     *
     * @param source the input configuration
     * @return the updated builder
     */
    Map<String, GenericActionConfig> resolveActions(InstrumentationSettings source) {
        Map<String, GenericActionConfig> resultMap = new HashMap<>();
        source.getActions().forEach((name, conf) -> {

            HashMap<String, String> additionalInputs = new HashMap<>(conf.getInput());

            val result = GenericActionConfig.builder()
                    .name(name)
                    .isVoid(conf.getIsVoid())
                    .importedPackages(conf.getImports());

            resolveSpecialVariables(additionalInputs, result);
            resolveArgumentVariables(additionalInputs, result);
            //everything remeinaing is a additional input variable
            additionalInputs.forEach(result::additionalArgumentType);
            resolveBody(conf, result);

            resultMap.put(name, result.build());
        });
        return resultMap;
    }

    private void resolveBody(GenericActionSettings conf, GenericActionConfig.GenericActionConfigBuilder result) {
        if (conf.getValue() != null) {
            if (conf.getIsVoid()) {
                result.valueBody(conf.getValue() + ";");
            } else {
                result.valueBody("return " + conf.getValue() + ";");
            }
        } else {
            result.valueBody(conf.getValueBody());
        }
    }


    private void resolveArgumentVariables(HashMap<String, String> inputs, GenericActionConfig.GenericActionConfigBuilder result) {
        val entryIterator = inputs.entrySet().iterator();
        while (entryIterator.hasNext()) {
            val varEntry = entryIterator.next();
            String varName = varEntry.getKey();
            if (GenericActionSettings.ARG_VARIABLE_REGEX.matcher(varName).matches()) {
                entryIterator.remove();
                int index = Integer.parseInt(varName.substring(GenericActionSettings.ARG_VARIABLE_PREFIX.length()));
                result.expectedArgumentType(index, varEntry.getValue());
            }
        }
    }

    private void resolveSpecialVariables(HashMap<String, String> inputs, GenericActionConfig.GenericActionConfigBuilder result) {
        result.expectedThisType(inputs.remove(GenericActionSettings.THIS_VARIABLE))
                .expectedReturnValueType(inputs.remove(GenericActionSettings.RETURN_VALUE_VARIABLE))
                .usesThrown(inputs.remove(GenericActionSettings.THROWN_VARIABLE) != null)
                .usesArgsArray(inputs.remove(GenericActionSettings.ARGS_VARIABLE) != null);
    }
}
