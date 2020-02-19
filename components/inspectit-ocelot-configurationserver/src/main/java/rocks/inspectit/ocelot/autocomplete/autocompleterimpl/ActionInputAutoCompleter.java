package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.YamlFileHelper;
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

    @Autowired
    private YamlFileHelper yamlFileHelper;

    private final static List<String> ACTION_PATH = Arrays.asList("inspectit", "instrumentation", "actions", "*");
    private final static List<String> ACTION_OPTIONS = Arrays.asList("entry", "exit", "preEntry", "postEntry", "preExit", "postExit");
    private static List<String> actionPathsToRetrieve = Arrays.asList("inspectit", "instrumentation", "rules", "*", "*", "*", "action");
    private final static List<String> INPUT_OPTIONS = Arrays.asList("data-input", "constant-input");

    /**
     * Checks if the given path leads to an action Attribute, e.g. "inspectit.instrumentation.actions.entry" and returns
     * all declared actions that could be used in this path as  List of Strings.
     *
     * @param path A given path as List. Each String should act as a literal of the path.
     * @return A List of Strings containing all declared rules that could be used with the given path.
     */
    @Override
    public List<String> getSuggestions(List<String> path) {
        ArrayList<String> toReturn = new ArrayList<>();
        if (PropertyPathHelper.comparePaths(path, ACTION_PATH)) {
            toReturn.addAll(getInput());
        }
        return toReturn;
    }

    /**
     * Searches all "action" attributes set in the yaml files present. And returns all declared rules.
     *
     * @return A List of Strings containing all declared rules.
     */
    @VisibleForTesting
    List<String> getInput() {
        ArrayList<String> toReturn = new ArrayList<>();
        for (String option : ACTION_OPTIONS) {
            actionPathsToRetrieve.set(4, option);
            for (String inputOption : INPUT_OPTIONS) {
                actionPathsToRetrieve.set(6, inputOption);
                toReturn.addAll(yamlFileHelper.extractKeysFromYamlFiles(new ArrayList<>(actionPathsToRetrieve)));
            }
        }
        return toReturn;
    }
}
