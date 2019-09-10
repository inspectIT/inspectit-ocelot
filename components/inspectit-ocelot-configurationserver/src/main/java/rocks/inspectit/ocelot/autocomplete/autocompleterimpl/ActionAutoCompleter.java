package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.YamlFileHelper;
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
    private YamlFileHelper yamlFileHelper;

    private final static List<String> ACTION_PATHS_A = Arrays.asList("inspectit", "instrumentation", "actions");
    private final static List<String> ACTION_PATHS_B = Arrays.asList("inspectit", "instrumentation", "rules", "*", "*", "*", "action");
    private final static List<String> ACTION_OPTIONS = Arrays.asList("entry", "exit", "preEntry", "postEntry", "preExit", "postExit");
    private static List<String> actionPathsToRetrieve = Arrays.asList("inspectit", "instrumentation", "rules", "*", "*", "*", "action");


    @Override
    public List<String> getSuggestions(List<String> path) {
        ArrayList<String> toReturn = new ArrayList<>();
        if (PropertyPathHelper.comparePaths(path, ACTION_PATHS_A) || PropertyPathHelper.comparePaths(path, ACTION_PATHS_B)) {
            toReturn.addAll(getActions());
        }
        return toReturn;
    }

    /**
     * Returns the actions defined in the yaml files
     *
     * @return
     */
    private List<String> getActions() {
        actionPathsToRetrieve.set(6, "action");
        return ACTION_OPTIONS.stream()
                .map(option -> actionPathsToRetrieve.set(4, option))
                .flatMap(opt -> yamlFileHelper.extractKeysFromYamlFiles(new ArrayList<>(actionPathsToRetrieve)).stream())
                .collect(Collectors.toList());
    }
}
