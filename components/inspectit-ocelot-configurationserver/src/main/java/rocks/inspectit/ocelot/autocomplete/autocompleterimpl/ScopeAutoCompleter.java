package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This AutoCompleter retrieves all scopes which can be found in the present yaml-files. It is either triggered by
 * the path "inspectit.instrumentation.scopes" or "inspectit.instrumentation.rules.*.scopes"
 */
@Slf4j
@Component
public class ScopeAutoCompleter implements AutoCompleter {

    private final static List<String> SCOPE_DECLARATION_PATH = Arrays.asList("inspectit", "instrumentation", "scopes");

    private final static List<List<String>> SCOPE_USAGE_PATHS = Arrays.asList(
            SCOPE_DECLARATION_PATH,
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "scopes"),
            Arrays.asList("inspectit", "instrumentation", "scopes", "*", "exclude")
    );

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

    @Override
    public List<String> getSuggestions(List<String> path) {
        if (SCOPE_USAGE_PATHS.stream().
                anyMatch(actionUsagePath -> PropertyPathHelper.comparePaths(path, actionUsagePath))) {
            return getScopes();
        }
        return Collections.emptyList();
    }

    /**
     * Searches through all yaml files and returns the scopes defined in them as Strings.
     *
     * @return A list of Strings resembling scopes defined in the yaml files.
     */
    private List<String> getScopes() {
        return configurationQueryHelper.getKeysForPath(SCOPE_DECLARATION_PATH);
    }
}
