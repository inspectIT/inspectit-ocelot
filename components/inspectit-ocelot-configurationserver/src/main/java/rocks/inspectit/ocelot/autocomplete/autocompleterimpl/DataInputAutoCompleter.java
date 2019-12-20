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
 * This AutoCompleter retrieves all data-inputs which can be found in the present yaml-files. It is triggered by
 * the path "inspectit.instrumentation.rules.*.entry.*.data-input"
 */
@Component
public class DataInputAutoCompleter implements AutoCompleter {

    @Autowired
    private YamlFileHelper yamlFileHelper;

    private static final List<String> ACTION_INPUT_PATH = Arrays.asList("inspectit", "instrumentation", "rules", "*", "entry", "*", "data-input");
    private List<String> actionPath = Arrays.asList("inspectit", "instrumentation", "rules", "*", "entry", "*", "action");
    private List<String> inputPath = Arrays.asList("inspectit", "instrumentation", "actions", "*", "input");

    /**
     * Returns suggestions if the given path is "inspectit.instrumentation.rules.RULE_NAME.entry.DATA_NAME.data-input"
     * The given suggestions are retrieved from the declared inputs found for all actions present in
     * "inspectit.instrumentation.rules.RULE_NAME.entry.DATA_NAME.action"
     *
     * @param path the path for which suggestions should be provided
     * @return
     */
    @Override
    public List<String> getSuggestions(List<String> path) {
        if (PropertyPathHelper.comparePaths(path, ACTION_INPUT_PATH)) {
            return getActionInputs(path);
        }
        return new ArrayList<>();
    }

    /**
     * Returns the rule name given in a path
     *
     * @param path the path for which suggestions should be provided
     * @return
     */
    private String getRuleFromPath(List<String> path) {
        if (path.size() > 4) {
            return path.get(3);
        }
        return null;
    }

    /**
     * Return the data-name given in a path
     *
     * @param path the path for which suggestions should be provided
     * @return
     */
    private String getDataNameFromPath(List<String> path) {
        if (path.size() > 6) {
            return path.get(5);
        }
        return null;
    }

    /**
     * Returns a List of found action names
     *
     * @param path the path for which suggestions should be provided
     * @return
     */
    private List<String> getActions(List<String> path) {
        actionPath.set(3, getRuleFromPath(path));
        actionPath.set(5, getDataNameFromPath(path));
        return yamlFileHelper.extractKeysFromYamlFiles(new ArrayList<>(actionPath));
    }

    /**
     * Returns the inputs defined for the actions in a given path
     *
     * @param path the path for which suggestions should be provided
     * @return
     */
    private List<String> getActionInputs(List<String> path) {
        return getActions(new ArrayList<>(path)).stream()
                .map(action -> inputPath.set(3, action))
                .flatMap(opt -> yamlFileHelper
                        .extractKeysFromYamlFiles(new ArrayList<>(inputPath))
                        .stream())
                .collect(Collectors.toList());
    }

}
