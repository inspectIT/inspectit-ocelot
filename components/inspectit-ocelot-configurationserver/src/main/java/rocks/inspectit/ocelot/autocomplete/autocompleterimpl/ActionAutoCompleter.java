package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This AutoCompleter retrieves all actions which can be found in the present yaml-files. It is either triggered by
 * the path "inspectit.instrumentation.actions" or "inspectit.instrumentation.rules.*.*.*.action"
 */
@Component
public class ActionAutoCompleter implements AutoCompleter {

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

    private final static List<String> ACTION_PATHS_A = Arrays.asList("inspectit", "instrumentation", "actions");
    private final static List<String> ACTION_PATHS_B = Arrays.asList("inspectit", "instrumentation", "rules", "*", "*", "*", "action");
    private final static List<String> ACTION_OPTIONS = Arrays.asList("entry", "exit", "preEntry", "postEntry", "preExit", "postExit");
    private static List<String> actionPathsToRetrieve = Arrays.asList("inspectit", "instrumentation", "rules", "*", "*", "*", "action");


    /**
     * Takes a list of Strings resembling a path as an argument. If the given path is an action path, all yaml files are
     * searched for defined actions. The found actions are returned as a List of String.
     * <p>
     * The accepted action paths are: "inspectit.instrumentation.rules.*.*.*.action" and
     * "inspectit.instrumentation.actions". '*' serves as a wild card here.
     *
     * @param path A given path as List. Each String should act as a literal of the path
     * @return A list of Strings resembling actions defined in the yaml files.
     */
    @Override
    public List<String> getSuggestions(List<String> path) {
        ArrayList<String> toReturn = new ArrayList<>();
        if (PropertyPathHelper.comparePaths(path, ACTION_PATHS_A) || PropertyPathHelper.comparePaths(path, ACTION_PATHS_B)) {
            toReturn.addAll(getActions());
        }
        return toReturn;
    }

    /**
     * Searches through all yaml files and returns the actions defined in them as Strings.
     *
     * @return A list of Strings resembling actions defined in the yaml files.
     */
    private List<String> getActions() {
        actionPathsToRetrieve.set(6, "action");
        return ACTION_OPTIONS.stream()
                .map(option -> actionPathsToRetrieve.set(4, option))
                .flatMap(opt -> configurationQueryHelper.getKeysForPath(new ArrayList<>(actionPathsToRetrieve)).stream())
                .collect(Collectors.toList());
    }
}