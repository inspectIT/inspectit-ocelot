package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import static rocks.inspectit.ocelot.autocomplete.autocompleterimpl.Constants.INSPECTIT;
import static rocks.inspectit.ocelot.autocomplete.autocompleterimpl.Constants.INSTRUMENTATION;
import static rocks.inspectit.ocelot.autocomplete.autocompleterimpl.Constants.RULES;
import static rocks.inspectit.ocelot.autocomplete.autocompleterimpl.Constants.ACTION_S;
import static rocks.inspectit.ocelot.autocomplete.autocompleterimpl.Constants.ACTION;
import static rocks.inspectit.ocelot.autocomplete.autocompleterimpl.Constants.STAR;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This AutoCompleter retrieves all actions which can be found in the present yaml-files. It is either triggered by
 * the path "inspectit.instrumentation.actions" or "inspectit.instrumentation.rules.*.*.*.action"
 */
@Component
public class ActionAutoCompleter implements AutoCompleter {

    private static final List<String> ACTION_DECLARATION_PATH = Collections.unmodifiableList(Arrays.asList(INSPECTIT, INSTRUMENTATION, ACTION_S));

    private static final List<List<String>> ACTION_USAGE_PATHS = Collections.unmodifiableList(Arrays.asList(
            ACTION_DECLARATION_PATH,
            Collections.unmodifiableList(Arrays.asList(INSPECTIT, INSTRUMENTATION, RULES, STAR, "preEntry", STAR, ACTION)),
            Collections.unmodifiableList(Arrays.asList(INSPECTIT, INSTRUMENTATION, RULES, STAR, "entry", STAR, ACTION)),
            Collections.unmodifiableList(Arrays.asList(INSPECTIT, INSTRUMENTATION, RULES, STAR, "exit", STAR, ACTION)),
            Collections.unmodifiableList(Arrays.asList(INSPECTIT, INSTRUMENTATION, RULES, STAR, "preEntry", STAR, ACTION)),
            Collections.unmodifiableList(Arrays.asList(INSPECTIT, INSTRUMENTATION, RULES, STAR, "postEntry", STAR, ACTION)),
            Collections.unmodifiableList(Arrays.asList(INSPECTIT, INSTRUMENTATION, RULES, STAR, "preExit", STAR, ACTION)),
            Collections.unmodifiableList(Arrays.asList(INSPECTIT, INSTRUMENTATION, RULES, STAR, "postExit", STAR, ACTION))
            ));

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

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
        if (ACTION_USAGE_PATHS.stream().
                anyMatch(actionUsagePath -> PropertyPathHelper.comparePaths(path, actionUsagePath))) {
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
        return configurationQueryHelper.getKeysForPath(ACTION_DECLARATION_PATH);
    }
}