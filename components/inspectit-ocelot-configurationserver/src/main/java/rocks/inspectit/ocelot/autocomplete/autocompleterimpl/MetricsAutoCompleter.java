package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class MetricsAutoCompleter implements AutoCompleter {

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

    /**
     * The path under which metric names are defined.
     */
    private final static List<String> METRICS_DECLARATION_PATH = Arrays.asList("inspectit", "instrumentation", "metrics");

    /**
     * All paths under which rule names are used.
     */
    private static final List<List<String>> RULE_SUGGESTION_PATHS = Arrays.asList(
            METRICS_DECLARATION_PATH,
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "metrics")
    );

    /**
     * Checks if the given path leads to a rule Attribute, e.g. "inspectit.instrumentation.rules" and returns
     * all declared rules that could be used in this path as  List of Strings.
     *
     * @param path A given path as List. Each String should act as a literal of the path.
     * @return A List of Strings containing all declared rules that could be used with the given path.
     */

    @Override
    public List<String> getSuggestions(List<String> path) {
        boolean matches = RULE_SUGGESTION_PATHS.stream()
                .anyMatch(rulePath -> PropertyPathHelper.comparePaths(path, rulePath));
        if (matches) {
            return configurationQueryHelper.getKeysForPath(METRICS_DECLARATION_PATH);
        }
        return Collections.emptyList();
    }
}
