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

    private final static String SECTION_PLACEHOLDER = "SECTION_PLACEHOLDER";

    private final static String INPUT_PLACEHOLDER = "INPUT_PLACEHOLDER";

    private final static String ACTION_PLACEHOLDER = "ACTION_PLACEHOLDER";

    private final static List<String> ACTION_OPTIONS = Arrays.asList(
            "entry",
            "exit",
            "preEntry",
            "postEntry",
            "preExit",
            "postExit"
    );

    private final static List<String> INPUT_OPTIONS = Arrays.asList("data-input", "constant-input");

    private final static List<String> ACTION_INPUT_DECLARATION_PATH = Arrays.asList(
            "inspectit",
            "instrumentation",
            "actions",
            ACTION_PLACEHOLDER,
            "input"
    );

    private final static List<String> ACTION_INPUT_DEFAULT_USAGE_PATH = Arrays.asList(
            "inspectit",
            "instrumentation",
            "rules",
            "*",
            SECTION_PLACEHOLDER,
            "*",
            INPUT_PLACEHOLDER);

    private static List<List<String>> actionInputUsagePaths;

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

    static {
        setupUsagePaths();
    }

    /**
     * Sets up the actionInputUsagePaths variable. To do so, all valid path combinations of actionInputDefaultUsagePath,
     * ACTION_OPTIONS and INPUT_OPTIONS are build and saved into this List.
     */
    private static void setupUsagePaths() {
        actionInputUsagePaths = new ArrayList<>();
        List<String> actionInputDefaultUsagePath = new ArrayList<>(ACTION_INPUT_DEFAULT_USAGE_PATH);

        for (String option : ACTION_OPTIONS) {
            actionInputDefaultUsagePath.set(ACTION_INPUT_DEFAULT_USAGE_PATH.indexOf(SECTION_PLACEHOLDER), option);

            for (String inputOption : INPUT_OPTIONS) {
                actionInputDefaultUsagePath.set(ACTION_INPUT_DEFAULT_USAGE_PATH.indexOf(INPUT_PLACEHOLDER), inputOption);
                actionInputUsagePaths.add(new ArrayList<>(actionInputDefaultUsagePath));
            }
        }
    }

    /**
     * Checks if the given path leads to an action attribute, e.g.
     * "inspectit.instrumentation.rules.my-rule.entry.my-entry-method.data-input" and returns
     * all declared action inputs that could be used in this path as List of Strings.
     * Ignores action inputs that start with the String defined in INPUT_FILTER_ONSET.
     *
     * @param path A given path as List. Each String should act as a literal of the path.
     * @return A List of Strings resembling the declared actions that could be used with the given path.
     */
    @Override
    public List<String> getSuggestions(List<String> path) {
        if (actionInputUsagePaths.stream().
                anyMatch(actionUsagePath -> PropertyPathHelper.comparePaths(path, actionUsagePath))) {
            return getActionInputs(path);
        }
        return Collections.emptyList();
    }

    /**
     * Takes a List of Strings resembling a path and returns all declared action inputs that could be used in this
     * path as List of Strings. Removes doubled entries. Filters out entries that start with the String defined in
     * INPUT_FILTER_ONSET.
     *
     * @param path A given path as List. Each String should act as a literal of the path.
     * @return A List of Strings resembling the declared actions that could be used with the given path.
     */
    private List<String> getActionInputs(List<String> path) {
        return buildActionPaths(path).stream()
                .flatMap(actionPath -> configurationQueryHelper.getKeysForPath(actionPath).stream())
                .distinct()
                .filter(value -> !value.startsWith("_"))
                .collect(Collectors.toList());
    }

    /**
     * Takes a List of Strings resembling a path and returns a List containing Lists of Strings.
     * Each of these Lists resemble an inspectit path which leads to an action the given path refers to.
     * E.g. : "inspectit.instrumentation.rules.my_rule.entry.my_var.action" refers to "action_A" and "action_B"
     * When this path is given as a parameter in this method, a List of Lists containing
     * ["inspectit","instrumentation","actions","action_A","input"] and
     * ["inspectit","instrumentation","actions","action_B","input"] is returned.
     *
     * @param path A given path as List. Each String should act as a literal of the path.
     * @return A List containing String Lists. Each of these Lists resemble one path as described.
     */
    private List<List<String>> buildActionPaths(List<String> path) {
        List<String> actions = getActions(path);
        List<List<String>> actionPaths = new ArrayList<>();
        for (String action : actions) {
            List<String> actionPath = new ArrayList<>(ACTION_INPUT_DECLARATION_PATH);
            actionPath.set(ACTION_INPUT_DECLARATION_PATH.indexOf(ACTION_PLACEHOLDER), action);
            actionPaths.add(actionPath);
        }
        return actionPaths;
    }

    /**
     * Takes a List of Strings resembling a path to a rule-input and returns the actions declared in this rule.
     * E.g. if "inspectit.instrumentation.rules.my-rule.entry.my-entry-method.data-input" is given as a path, the values
     * found under "inspectit.instrumentation.rules.my-rule.entry.my-entry-method.action" are returned.
     *
     * @param path A List of Strings resembling a path to a rule-input.
     * @return The actions declared in this rule.
     */
    private List<String> getActions(List<String> path) {
        List<String> actionDeclarationPath = new ArrayList<>(path);
        actionDeclarationPath.set(ACTION_INPUT_DEFAULT_USAGE_PATH.indexOf(INPUT_PLACEHOLDER), "action");
        return configurationQueryHelper.getKeysForPath(actionDeclarationPath);
    }
}
