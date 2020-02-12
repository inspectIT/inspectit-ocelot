package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This AutoCompleter retrieves all rules which can be found in the present yaml-files. It is triggered by
 * the path "inspectit.instrumentation.rules"
 */
@Component
public class RuleAutoCompleter implements AutoCompleter {

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

    private final static List<String> RULE_PATH = Arrays.asList("inspectit", "instrumentation", "rules");

    @Override
    public List<String> getSuggestions(List<String> path) {
        if (PropertyPathHelper.comparePathsIgnoreCamelOrKebabCase(path, RULE_PATH)) {
            return configurationQueryHelper.extractKeysFromYamlFiles(RULE_PATH);
        }
        return Collections.emptyList();
    }
}
