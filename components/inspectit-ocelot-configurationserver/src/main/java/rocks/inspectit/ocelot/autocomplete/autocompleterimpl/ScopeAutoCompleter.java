package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This AutoCompleter retrieves all scopes which can be found in the present yaml-files. It is either triggered by
 * the path "inspectit.instrumentation.scopes" or "inspectit.instrumentation.rules.*.scopes"
 */
@Slf4j
@Component
public class ScopeAutoCompleter implements AutoCompleter {

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

    private final static List<String> SCOPE_PATHS_A = Arrays.asList("inspectit", "instrumentation", "scopes");
    private final static List<String> SCOPE_PATHS_B = Arrays.asList("inspectit", "instrumentation", "rules", "*", "scopes");

    @Override
    public List<String> getSuggestions(List<String> path) {
        if (PropertyPathHelper.hasPathPrefix(path, SCOPE_PATHS_A) || PropertyPathHelper.hasPathPrefix(path, SCOPE_PATHS_B)) {
            return configurationQueryHelper.getKeysForPath(path);
        }
        return new ArrayList<>();
    }
}
