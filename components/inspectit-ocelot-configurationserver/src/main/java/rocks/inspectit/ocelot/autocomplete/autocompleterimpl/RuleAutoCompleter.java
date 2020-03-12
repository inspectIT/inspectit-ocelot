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
 * the path "inspectit.instrumentation.rules".
 */
@Component
public class RuleAutoCompleter implements AutoCompleter {

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

    private final static List<String> RULE_PATH = Arrays.asList("inspectit", "instrumentation", "rules");

    /**
     * Checks if the given path leads to a rule Attribute, e.g. "inspectit.instrumentation.rules" and returns
     * all declared rules that could be used in this path as  List of Strings.
     *
     * @param path A given path as List. Each String should act as a literal of the path.
     * @return A List of Strings containing all declared rules that could be used with the given path.
     */

    @Override
    public List<String> getSuggestions(List<String> path) {
        if (PropertyPathHelper.comparePathsIgnoreCamelOrKebabCase(path, RULE_PATH)) {
            return configurationQueryHelper.getKeysForPath(RULE_PATH);
        }
        return Collections.emptyList();
    }
}
