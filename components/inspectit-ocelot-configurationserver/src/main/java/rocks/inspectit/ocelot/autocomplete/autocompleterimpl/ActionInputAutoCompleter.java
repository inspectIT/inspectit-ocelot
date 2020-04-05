package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This AutoCompleter retrieves all inputs for actions which can be found in the present yaml-files. It is triggered by
 * the path "inspectit.instrumentation.actions.*"
 */
@Component
public class ActionInputAutoCompleter implements AutoCompleter {

    private static List<String> ACTION_DECLARATION_PATH = Arrays.asList("inspectit", "instrumentation", "actions", "*");

    private static List<List<String>> ACTION_USAGE_PATHS = Arrays.asList(
            ACTION_DECLARATION_PATH,
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "preEntry", "constant-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "preEntry", "data-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "entry", "constant-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "entry", "data-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "exit", "constant-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "exit", "data-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "preEntry", "constant-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "preEntry", "data-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "postEntry", "constant-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "postEntry", "data-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "preExit", "constant-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "preExit", "data-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "postExit", "constant-input", "action"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "postExit", "data-input", "action")
    );

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

    /**
     * Checks if the given path leads to an action Attribute, e.g. "inspectit.instrumentation.actions.entry" and returns
     * all declared actions that could be used in this path as  List of Strings.
     *
     * @param path A given path as List. Each String should act as a literal of the path.
     * @return A List of Strings resembling the declared actions that could be used with the given path.
     */
    @Override
    public List<String> getSuggestions(List<String> path) {
        ArrayList<String> toReturn = new ArrayList<>();
        if (ACTION_USAGE_PATHS.stream().
                anyMatch(actionUsagePath -> PropertyPathHelper.comparePaths(path, actionUsagePath))) {
            toReturn.addAll(getInput());
        }
        return toReturn;
    }

    /**
     * Searches all "action" attributes set in the yaml files present and returns all declared actions as Strings.
     *
     * @return A List of Strings resembling all declared rules.
     */
    private List<String> getInput() {
        return configurationQueryHelper.getKeysForPath(ACTION_DECLARATION_PATH);
    }
}
