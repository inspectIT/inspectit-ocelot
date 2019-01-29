package rocks.inspectit.oce.core.instrumentation.config;

import lombok.val;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.GenericDataProviderSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedGenericDataProviderConfig;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.DataProviderGenerator;

import java.util.HashMap;
import java.util.Map;

@Component
public class DataProviderResolver {

    /**
     * Resolves {@link InstrumentationConfiguration#getDataProviders()}.
     * Returns a map mapping the names of providers to their resolved configurations
     *
     * @param builder the builder to resolve
     * @param source  the input configuration
     * @return the updated builder
     */
    Map<String, ResolvedGenericDataProviderConfig> resolveProviders(InstrumentationSettings source) {
        Map<String, ResolvedGenericDataProviderConfig> resultMap = new HashMap<>();
        for (val entry : source.getDataProviders().entrySet()) {
            String name = entry.getKey();
            val conf = entry.getValue();

            HashMap<String, String> additionalInputs = new HashMap<>(conf.getInput());

            val result = ResolvedGenericDataProviderConfig.builder()
                    .name(name)
                    .importedPackages(conf.getImports());

            resolveSpecialVariables(additionalInputs, result);
            resolveArgumentVariables(additionalInputs, result);
            resolveAdditionalInputs(additionalInputs, result);
            resolveBody(conf, result);

            resultMap.put(name, result.build());
        }
        return resultMap;
    }

    private void resolveBody(GenericDataProviderSettings conf, ResolvedGenericDataProviderConfig.ResolvedGenericDataProviderConfigBuilder result) {
        if (conf.getValue() != null) {
            result.valueBody("return " + conf.getValue() + ";");
        } else {
            result.valueBody(conf.getValueBody());
        }
    }

    private void resolveAdditionalInputs(HashMap<String, String> additionalInputs, ResolvedGenericDataProviderConfig.ResolvedGenericDataProviderConfigBuilder result) {
        //everything that has not been removed is an additional argument
        for (val additionalVarEntry : additionalInputs.entrySet()) {
            result.additionalArgumentType(additionalVarEntry.getKey(), additionalVarEntry.getValue());
        }
    }

    private void resolveArgumentVariables(HashMap<String, String> inputs, ResolvedGenericDataProviderConfig.ResolvedGenericDataProviderConfigBuilder result) {
        val entryIterator = inputs.entrySet().iterator();
        while (entryIterator.hasNext()) {
            val varEntry = entryIterator.next();
            String varName = varEntry.getKey();
            if (varName.matches(DataProviderGenerator.ARG_VARIABLE_PREFIX + "\\d+")) {
                entryIterator.remove();
                int index = Integer.parseInt(varName.substring(DataProviderGenerator.ARG_VARIABLE_PREFIX.length()));
                result.expectedArgumentType(index, varEntry.getValue());
            }
        }
    }

    private void resolveSpecialVariables(HashMap<String, String> inputs, ResolvedGenericDataProviderConfig.ResolvedGenericDataProviderConfigBuilder result) {
        result.expectedThisType(inputs.remove(DataProviderGenerator.THIZ_VARIABLE))
                .expectedReturnValueType(inputs.remove(DataProviderGenerator.RETURN_VALUE_VARIABLE))
                .usesThrown(inputs.remove(DataProviderGenerator.THROWN_VARIABLE) != null)
                .usesArgsArray(inputs.remove(DataProviderGenerator.ARGS_VARIABLE) != null);
    }
}
