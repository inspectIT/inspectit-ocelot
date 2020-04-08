package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This AutoCompleter retrieves all inputs for actions which can be found in the present yaml-files. It is triggered by
 * the path "inspectit.instrumentation.actions.*"
 */
@Component
public class ActionInputAutoCompleter implements AutoCompleter {

    private final static List<String> ACTION_OPTIONS = Arrays.asList("entry", "exit", "preEntry", "postEntry", "preExit", "postExit");

    private final static List<String> INPUT_OPTIONS = Arrays.asList("data-input", "constant-input");

    private final static List<String> ACTION_INPUT_DECLARATION_PATH = Arrays.asList("inspectit", "instrumentation", "actions", "*", "input");

    private static List<String> actionInputDefaultUsagePath = Arrays.asList("inspectit", "instrumentation", "rules", "*", "*", "*", "*");

    private static List<List<String>> actionInputUsagePaths;

    static {
        setupUsagePaths();
    }

    /**
     * Sets up the actionInputUsagePaths variable. To do so, all valid path combinations of actionInputDefaultUsagePath,
     * ACTION_OPTIONS and INPUT_OPTIONS are build and saved into this List.
     */
    private static void setupUsagePaths() {
        actionInputUsagePaths = new ArrayList<>();

        for (String option : ACTION_OPTIONS) {
            actionInputDefaultUsagePath.set(4, option);

            for (String inputOption : INPUT_OPTIONS) {
                actionInputDefaultUsagePath.set(6, inputOption);
                actionInputUsagePaths.add(new ArrayList<>(actionInputDefaultUsagePath));
            }
        }
    }

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
        if (actionInputUsagePaths.stream().
                anyMatch(actionUsagePath -> PropertyPathHelper.comparePaths(path, actionUsagePath))) {
            return getInput(path);
        }
        return Collections.emptyList();
    }

    /**
     * Searches all "action" attributes set in the yaml files present and returns all declared actions as Strings.
     *
     * @return A List of Strings resembling all declared rules.
     */
    private List<String> getInput(List<String> path) {
        return configurationQueryHelper.getKeysForPath(getInputDeclarationPath(path))
                .stream()
                .filter(value -> !value.startsWith("_"))
                .collect(Collectors.toList());
    }

    /**
     * Puts the method name found in a given path into a copy of the ACTION_INPUT_DECLARATION_PATH variable.
     *
     * @param path the path initially put into the method.
     * @return A List of Strings resembling a path leading to the methods input declarations.
     */
    private List<String> getInputDeclarationPath(List<String> path) {
        List<String> currentDeclarationPath = new ArrayList<>(ACTION_INPUT_DECLARATION_PATH);
        currentDeclarationPath.set(3, path.get(3));
        return currentDeclarationPath;
    }
}
