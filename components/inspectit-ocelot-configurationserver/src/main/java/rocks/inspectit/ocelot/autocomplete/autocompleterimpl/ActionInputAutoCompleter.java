package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

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

    @Override
    public List<String> getSuggestions(List<String> path) {
        ArrayList<String> toReturn = new ArrayList<>();
        if (PropertyPathHelper.comparePaths(path, ACTION_PATH)) {
            toReturn.addAll(getInput());
        }
        return toReturn;
    }

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
